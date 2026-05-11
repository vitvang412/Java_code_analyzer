package com.codeanalyzer.ui.panels;

import com.codeanalyzer.ai.CodeAnalyzer;
import com.codeanalyzer.ai.GeminiService;
import com.codeanalyzer.database.AnalysisResultDAO;
import com.codeanalyzer.database.StudentDAO;
import com.codeanalyzer.database.SubmissionDAO;
import com.codeanalyzer.model.AnalysisResult;
import com.codeanalyzer.model.Student;
import com.codeanalyzer.model.Submission;
import com.codeanalyzer.ui.UIConstants;
import com.codeanalyzer.ui.components.StyledButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for triggering Gemini AI analysis on unanalyzed submissions.
 * Left: real-time console log. Right: results table with click-to-detail.
 */
public class AnalysisPanel extends JPanel {

    private final SubmissionDAO    submissionDAO    = new SubmissionDAO();
    private final StudentDAO       studentDAO       = new StudentDAO();
    private final AnalysisResultDAO analysisResultDAO = new AnalysisResultDAO();
    private final CodeAnalyzer     analyzer         = new CodeAnalyzer();
    private final GeminiService    geminiService    = new GeminiService();

    // Console log
    private JTextPane      logPane;
    private StyledDocument logDoc;
    private JProgressBar   progressBar;
    private JSpinner       spinLimit;
    private JComboBox<StudentItem> cboStudent;
    private JLabel         statLabel;

    // Results table
    private DefaultTableModel resultsModel;
    private JTable            resultsTable;
    private final List<AnalysisResult> resultsList = new ArrayList<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    public AnalysisPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BACKGROUND);

        add(buildConfigCard(), BorderLayout.NORTH);

        // Split: console left | results right
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLogCard(), buildResultsCard());
        split.setResizeWeight(0.5);
        split.setDividerSize(5);
        split.setBorder(new EmptyBorder(0, 0, 0, 0));
        add(split, BorderLayout.CENTER);
    }

    // ── Config card ───────────────────────────────────────────────────────────

    private JPanel buildConfigCard() {
        JPanel card = new JPanel(new BorderLayout(12, 10));
        card.setBackground(UIConstants.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, UIConstants.ACCENT),
                new EmptyBorder(14, 20, 14, 20)));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel("Gemini AI Analysis");
        title.setFont(UIConstants.SUBHEADER);
        title.setForeground(UIConstants.PRIMARY_DARK);
        JLabel hint = new JLabel("Chọn tài khoản và batch size, nhấn Run.");
        hint.setFont(UIConstants.LABEL);
        hint.setForeground(UIConstants.TEXT_MUTED);
        hint.setHorizontalAlignment(SwingConstants.RIGHT);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(hint,  BorderLayout.EAST);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        row1.setOpaque(false);
        cboStudent = new JComboBox<>();
        cboStudent.setFont(UIConstants.MAIN);
        cboStudent.setPreferredSize(new Dimension(210, 32));
        loadStudentCombo();
        
        // Auto-refresh accounts when clicking the dropdown
        cboStudent.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                loadStudentCombo();
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });
        spinLimit = new JSpinner(new SpinnerNumberModel(10, 1, 200, 5));
        spinLimit.setFont(UIConstants.MAIN);
        ((JSpinner.DefaultEditor) spinLimit.getEditor()).getTextField().setColumns(5);
        row1.add(makeLabel("Tài khoản:"));
        row1.add(cboStudent);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(makeLabel("Batch size:"));
        row1.add(spinLimit);

        JPanel row2 = new JPanel(new BorderLayout(12, 0));
        row2.setOpaque(false);
        row2.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setOpaque(false);
        StyledButton btnStart  = new StyledButton("Run AI Analysis", StyledButton.Variant.SUCCESS);
        StyledButton btnTest   = new StyledButton("Test Gemini API",  StyledButton.Variant.ACCENT);
        StyledButton btnClear  = new StyledButton("Clear Log",           StyledButton.Variant.NEUTRAL);
        StyledButton btnReload = new StyledButton("Reload Accounts",  StyledButton.Variant.NEUTRAL);
        StyledButton btnRefreshResults = new StyledButton("Load Results", StyledButton.Variant.NEUTRAL);
        btnStart.addActionListener(e         -> startAnalysis(btnStart));
        btnTest.addActionListener(e          -> testGeminiApi(btnTest));
        btnClear.addActionListener(e         -> clearLog());
        btnReload.addActionListener(e        -> { loadStudentCombo(); JOptionPane.showMessageDialog(this, "Đã cập nhật danh sách tài khoản!"); });
        btnRefreshResults.addActionListener(e -> refreshResults());
        btnPanel.add(btnStart);
        btnPanel.add(btnTest);
        btnPanel.add(btnClear);
        btnPanel.add(btnReload);
        btnPanel.add(btnRefreshResults);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Idle");
        progressBar.setFont(UIConstants.SMALL);
        progressBar.setForeground(UIConstants.ACCENT);
        progressBar.setBackground(UIConstants.BACKGROUND);
        progressBar.setPreferredSize(new Dimension(0, 22));
        statLabel = new JLabel("Chưa có phân tích nào đang chạy.");
        statLabel.setFont(UIConstants.SMALL);
        statLabel.setForeground(UIConstants.TEXT_SECONDARY);

        JPanel progressPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        progressPanel.setOpaque(false);
        progressPanel.add(progressBar);
        progressPanel.add(statLabel);

        row2.add(btnPanel,      BorderLayout.WEST);
        row2.add(progressPanel, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setOpaque(false);
        content.add(row1, BorderLayout.NORTH);
        content.add(row2, BorderLayout.SOUTH);
        card.add(titleRow, BorderLayout.NORTH);
        card.add(content,  BorderLayout.CENTER);
        return card;
    }

    // ── Console log card ──────────────────────────────────────────────────────

    private JPanel buildLogCard() {
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(UIConstants.MONO);
        logPane.setBackground(UIConstants.CONSOLE_BG);
        logPane.setForeground(UIConstants.CONSOLE_FG);
        logPane.setCaretColor(UIConstants.ACCENT_LIGHT);
        logPane.setBorder(new EmptyBorder(10, 14, 10, 14));
        logDoc = logPane.getStyledDocument();

        JScrollPane sp = new JScrollPane(logPane);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(UIConstants.CONSOLE_BG);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(new Color(0x161B22));
        headerBar.setBorder(new EmptyBorder(6, 14, 6, 14));
        JLabel headerLbl = new JLabel("● Analysis Console");
        headerLbl.setFont(UIConstants.SMALL_BOLD);
        headerLbl.setForeground(new Color(0x8B949E));
        JLabel tipLbl = new JLabel("Xanh = OK  |  Đỏ = Lỗi  |  Vàng = Cảnh báo");
        tipLbl.setFont(UIConstants.LABEL);
        tipLbl.setForeground(new Color(0x484F58));
        headerBar.add(headerLbl, BorderLayout.WEST);
        headerBar.add(tipLbl,    BorderLayout.EAST);

        JPanel inner = new JPanel(new BorderLayout());
        inner.add(headerBar, BorderLayout.NORTH);
        inner.add(sp,        BorderLayout.CENTER);
        inner.setBorder(BorderFactory.createLineBorder(new Color(0x30363D)));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UIConstants.CONSOLE_BG);
        card.setBorder(new EmptyBorder(10, 18, 10, 6));
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    // ── Results table card ────────────────────────────────────────────────────

    private JPanel buildResultsCard() {
        String[] cols = {"Username", "Bài tập", "AI Score", "Complexity", "Thời gian"};
        resultsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        resultsTable = new JTable(resultsModel) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? UIConstants.CARD_BG : UIConstants.TABLE_ALT_ROW);
                    c.setForeground(UIConstants.TEXT);
                } else {
                    c.setBackground(UIConstants.TABLE_SEL_BG);
                    c.setForeground(UIConstants.TABLE_SEL_FG);
                }
                return c;
            }
        };
        styleTable(resultsTable);

        // AI Score column — color by severity
        resultsTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(SwingConstants.CENTER); }
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean f, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, f, row, col);
                if (!sel) {
                    try {
                        int score = Integer.parseInt(String.valueOf(v).replace("/10","").trim());
                        if (score >= 7) { setBackground(UIConstants.DANGER_LIGHT);  setForeground(UIConstants.DANGER); }
                        else if (score >= 4) { setBackground(UIConstants.WARNING_LIGHT); setForeground(UIConstants.WARNING); }
                        else { setBackground(UIConstants.SUCCESS_LIGHT); setForeground(UIConstants.SUCCESS); }
                    } catch (Exception ex) {
                        setBackground(UIConstants.CARD_BG); setForeground(UIConstants.TEXT);
                    }
                    setFont(UIConstants.SMALL_BOLD);
                }
                return this;
            }
        });

        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        resultsTable.getColumnModel().getColumn(2).setMaxWidth(80);
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        resultsTable.getColumnModel().getColumn(4).setMaxWidth(90);

        // Double-click or single-click select -> show detail
        resultsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) showDetailForRow(resultsTable.getSelectedRow());
            }
        });

        JScrollPane sp = new JScrollPane(resultsTable);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(UIConstants.CARD_BG);

        // Header bar
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(UIConstants.TABLE_HEADER_BG);
        headerBar.setBorder(new EmptyBorder(6, 14, 6, 14));
        JLabel lbl = new JLabel("📊  Kết quả phân tích  —  double-click để xem chi tiết");
        lbl.setFont(UIConstants.SMALL_BOLD);
        lbl.setForeground(UIConstants.TABLE_HEADER_FG);
        headerBar.add(lbl, BorderLayout.WEST);

        JPanel inner = new JPanel(new BorderLayout());
        inner.add(headerBar, BorderLayout.NORTH);
        inner.add(sp,        BorderLayout.CENTER);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UIConstants.BACKGROUND);
        card.setBorder(new EmptyBorder(10, 6, 10, 18));
        card.add(inner, BorderLayout.CENTER);

        // Load existing results on startup
        refreshResults();
        return card;
    }

    // ── Detail dialog ─────────────────────────────────────────────────────────

    private void showDetailForRow(int row) {
        if (row < 0 || row >= resultsList.size()) return;
        AnalysisResult r = resultsList.get(row);

        int ai = r.getAiUsageScore();
        String aiColor = ai >= 7 ? "#C62828" : ai >= 4 ? "#E65100" : "#1B5E20";
        String aiLabel = ai >= 7 ? "Cao – nghi dùng AI" : ai >= 4 ? "Trung bình" : "Thấp – tự viết";

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:Segoe UI;font-size:13px;margin:12px;'>");
        sb.append("<h2 style='color:#1A237E;margin-bottom:2px;'>").append(esc(r.getProblemName())).append("</h2>");
        sb.append("<p style='color:#546E7A;margin-top:0;'>").append(esc(r.getStudentUsername())).append("</p>");

        // AI Score block
        sb.append("<div style='background:#F3F4F6;border-left:4px solid ").append(aiColor)
          .append(";padding:10px 14px;margin:10px 0;border-radius:4px;'>");
        sb.append("<b style='font-size:15px;color:").append(aiColor).append(";'>AI Usage Score: ")
          .append(ai).append(" / 10</b>");
        sb.append("<br><span style='color:#37474F;'>Mức: ").append(aiLabel).append("</span>");
        if (r.getAiUsageReason() != null && !r.getAiUsageReason().isBlank()) {
            sb.append("<br><br><b>Lý do Gemini đánh giá:</b><br>")
              .append("<span style='color:#424242;'>").append(esc(r.getAiUsageReason())).append("</span>");
        }
        sb.append("</div>");

        // Info table
        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:10px;'>");
        sb.append(infoRow("⏱ Complexity",    r.getComplexityEstimate()));
        sb.append(infoRow("🔧 Algorithms",    r.getAlgorithms() != null ? r.getAlgorithms().replaceAll("[\\[\\]\"]","") : "—"));
        sb.append(infoRow("🗄 Data Structures", r.getDataStructures() != null ? r.getDataStructures().replaceAll("[\\[\\]\"]","") : "—"));
        if (r.getAnalyzedAt() != null) {
            sb.append(infoRow("📅 Analyzed At",  r.getAnalyzedAt().format(DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy"))));
        }
        sb.append("</table>");

        // Summary
        if (r.getSummary() != null && !r.getSummary().isBlank()) {
            sb.append("<b style='color:#1A237E;'>📝 Tóm tắt code:</b>");
            sb.append("<div style='background:#FAFAFA;border:1px solid #E0E0E0;padding:10px;"
                    + "border-radius:4px;margin-top:6px;color:#333;line-height:1.6;'>");
            sb.append(esc(r.getSummary()).replace("\n", "<br>"));
            sb.append("</div>");
        }
        sb.append("</body></html>");

        JEditorPane pane = new JEditorPane("text/html", sb.toString());
        pane.setEditable(false);
        pane.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(pane);
        scrollPane.setPreferredSize(new Dimension(680, 480));
        JOptionPane.showMessageDialog(this, scrollPane,
                "Chi tiết: " + r.getProblemName(), JOptionPane.PLAIN_MESSAGE);
    }

    private String infoRow(String key, String val) {
        if (val == null || val.isBlank()) val = "—";
        return "<tr style='border-bottom:1px solid #EEE;'>"
             + "<td style='color:#546E7A;padding:6px 12px 6px 0;white-space:nowrap;width:140px;'><b>" + key + "</b></td>"
             + "<td style='color:#212121;padding:6px 0;'>" + esc(val) + "</td></tr>";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void refreshResults() {
        resultsList.clear();
        resultsModel.setRowCount(0);
        List<AnalysisResult> all = analysisResultDAO.findAll(200);
        for (AnalysisResult r : all) {
            resultsList.add(r);
            resultsModel.addRow(new Object[]{
                r.getStudentUsername(),
                r.getProblemName(),
                r.getAiUsageScore() + "/10",
                r.getComplexityEstimate() != null ? r.getComplexityEstimate() : "—",
                r.getAnalyzedAt() != null ? r.getAnalyzedAt().format(DT_FMT) : "—"
            });
        }
    }

    private void loadStudentCombo() {
        Object selObj = cboStudent.getSelectedItem();
        int selId = (selObj instanceof StudentItem) ? ((StudentItem) selObj).id() : 0;

        cboStudent.removeAllItems();
        StudentItem all = new StudentItem(0, "Tất cả tài khoản");
        cboStudent.addItem(all);
        StudentItem toSelect = all;

        for (Student s : studentDAO.findAll()) {
            StudentItem item = new StudentItem(s.getId(), s.getUsername());
            cboStudent.addItem(item);
            if (item.id() == selId) {
                toSelect = item;
            }
        }
        cboStudent.setSelectedItem(toSelect);
    }

    private void testGeminiApi(JButton btn) {
        btn.setEnabled(false);
        appendLog("──────────────────────────────────────────────", LogLevel.DIVIDER);
        appendLog("[TEST] Đang kiểm tra kết nối Gemini API...", LogLevel.INFO);
        new Thread(() -> {
            try {
                String result = geminiService.generate("Reply with exactly: {\"status\":\"ok\"}");
                appendLog("[TEST] ✅ Gemini API hoạt động tốt!", LogLevel.SUCCESS);
                appendLog("[TEST] Response: " + result, LogLevel.INFO);
            } catch (Exception ex) {
                appendLog("[TEST] ❌ Gemini API lỗi: " + ex.getMessage(), LogLevel.ERROR);
                appendLog("       • API key sai hoặc hết quota.", LogLevel.WARN);
                appendLog("       • Không có kết nối internet.", LogLevel.WARN);
            } finally {
                SwingUtilities.invokeLater(() -> btn.setEnabled(true));
            }
        }, "gemini-test-thread").start();
    }

    private void startAnalysis(JButton btnStart) {
        btnStart.setEnabled(false);
        int limit = (int) spinLimit.getValue();

        new Thread(() -> {
            List<Submission> pending;
            StudentItem sel = (StudentItem) cboStudent.getSelectedItem();
            if (sel != null && sel.id != 0) {
                pending = submissionDAO.findUnanalyzedByStudent(sel.id, limit);
            } else {
                pending = submissionDAO.findUnanalyzed(limit);
            }

            appendLog("══════════════════════════════════════════════════", LogLevel.DIVIDER);
            appendLog(String.format("[START] %s — batch size: %d",
                    LocalDateTime.now().format(TIME_FMT), limit), LogLevel.INFO);

            if (pending.isEmpty()) {
                appendLog("[INFO] Không tìm thấy bài nào cần phân tích.", LogLevel.WARN);
                appendLog("       • Source code NULL/rỗng hoặc tất cả đã phân tích rồi.", LogLevel.WARN);
                SwingUtilities.invokeLater(() -> {
                    btnStart.setEnabled(true);
                    progressBar.setValue(0);
                    progressBar.setString("Không có bài cần phân tích.");
                    statLabel.setText("Không tìm thấy submission nào có source code.");
                });
                return;
            }

            appendLog(String.format("[INFO] Tìm thấy %d bài. Bắt đầu gửi lên Gemini...",
                    pending.size()), LogLevel.INFO);

            int total = pending.size(), success = 0, failed = 0;

            for (int i = 0; i < total; i++) {
                Submission s = pending.get(i);
                final int idx = i;
                SwingUtilities.invokeLater(() -> {
                    int pct = (int) (100.0 * idx / total);
                    progressBar.setValue(pct);
                    progressBar.setString(String.format("%d / %d  (%d%%)", idx + 1, total, pct));
                    statLabel.setText("Đang phân tích: " + s.getProblemName()
                            + "  (User: " + s.getStudentUsername() + ")");
                });

                appendLog(String.format("[%d/%d] Bài: %-30s | User: %s",
                        i + 1, total, s.getProblemName(), s.getStudentUsername()), LogLevel.INFO);

                if (s.getSourceCode() == null || s.getSourceCode().isBlank()) {
                    appendLog("       ⚠ Bỏ qua: source_code rỗng.", LogLevel.WARN);
                    failed++;
                    continue;
                }

                try {
                    var result = analyzer.analyzeSubmission(s);
                    if (result != null) {
                        appendLog(String.format("       ✅ AI score: %d/10  | Complexity: %s  | %s",
                                result.getAiUsageScore(),
                                result.getComplexityEstimate(),
                                result.getAlgorithms() != null
                                        ? result.getAlgorithms().replaceAll("[\\[\\]\"]","") : ""), LogLevel.SUCCESS);
                        success++;
                        SwingUtilities.invokeLater(this::refreshResults);
                    } else {
                        appendLog("       ❌ Phân tích trả về null.", LogLevel.ERROR);
                        failed++;
                    }
                } catch (Exception ex) {
                    appendLog("       ❌ Lỗi: " + ex.getMessage(), LogLevel.ERROR);
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        appendLog("         Caused by: " + cause.getMessage(), LogLevel.ERROR);
                        cause = cause.getCause();
                    }
                    failed++;
                }

                try {
                    // Gemini Free Tier: ~13 RPM để tránh 429 Quota errors
                    Thread.sleep(4500);
                } catch (InterruptedException ignored) {}
            }

            final int fSuccess = success, fFailed = failed;
            appendLog("══════════════════════════════════════════════════", LogLevel.DIVIDER);
            appendLog(String.format("[DONE] Tổng: %d bài  |  ✅ %d thành công  |  ❌ %d lỗi",
                    total, fSuccess, fFailed), fFailed > 0 ? LogLevel.WARN : LogLevel.SUCCESS);

            SwingUtilities.invokeLater(() -> {
                btnStart.setEnabled(true);
                progressBar.setValue(100);
                progressBar.setString("Hoàn tất!");
                statLabel.setText(String.format("Xong: %d/%d bài (%d lỗi)", fSuccess, total, fFailed));
                refreshResults();
            });
        }, "analysis-thread").start();
    }

    // ── Logging helpers ───────────────────────────────────────────────────────

    private enum LogLevel { INFO, SUCCESS, WARN, ERROR, DIVIDER }

    private void clearLog() {
        try { logDoc.remove(0, logDoc.getLength()); } catch (BadLocationException ignored) {}
    }

    private void appendLog(String msg, LogLevel level) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attr = new SimpleAttributeSet();
                Color fg = switch (level) {
                    case SUCCESS -> UIConstants.CONSOLE_SUCCESS;
                    case WARN    -> UIConstants.CONSOLE_WARN;
                    case ERROR   -> UIConstants.CONSOLE_ERROR;
                    case DIVIDER -> new Color(0x30363D);
                    default      -> UIConstants.CONSOLE_FG;
                };
                StyleConstants.setForeground(attr, fg);
                StyleConstants.setFontFamily(attr, "Consolas");
                StyleConstants.setFontSize(attr, 12);
                logDoc.insertString(logDoc.getLength(), msg + "\n", attr);
                logPane.setCaretPosition(logDoc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void styleTable(JTable t) {
        t.setRowHeight(28);
        t.setFont(UIConstants.MAIN);
        t.setGridColor(UIConstants.TABLE_GRID);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(UIConstants.TABLE_SEL_BG);
        t.setSelectionForeground(UIConstants.TABLE_SEL_FG);
        t.getTableHeader().setFont(UIConstants.SMALL_BOLD);
        t.getTableHeader().setBackground(UIConstants.TABLE_HEADER_BG);
        t.getTableHeader().setForeground(UIConstants.TABLE_HEADER_FG);
        t.getTableHeader().setPreferredSize(new Dimension(0, 32));
        t.getTableHeader().setReorderingAllowed(false);
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.SMALL_BOLD);
        l.setForeground(UIConstants.TEXT_SECONDARY);
        return l;
    }

    private record StudentItem(int id, String name) {
        @Override public String toString() { return name; }
    }
}
