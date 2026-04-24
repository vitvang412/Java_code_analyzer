package com.codeanalyzer.ui.panels;

import com.codeanalyzer.ai.CodeAnalyzer;
import com.codeanalyzer.ai.GeminiService;
import com.codeanalyzer.database.StudentDAO;
import com.codeanalyzer.database.SubmissionDAO;
import com.codeanalyzer.model.Student;
import com.codeanalyzer.model.Submission;
import com.codeanalyzer.ui.UIConstants;
import com.codeanalyzer.ui.components.StyledButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel for triggering Gemini AI analysis on unanalyzed submissions.
 * Shows real-time progress and a color-coded console log.
 */
public class AnalysisPanel extends JPanel {

    private final SubmissionDAO submissionDAO = new SubmissionDAO();
    private final StudentDAO    studentDAO    = new StudentDAO();
    private final CodeAnalyzer  analyzer      = new CodeAnalyzer();
    private final GeminiService geminiService = new GeminiService();

    private JTextPane    logPane;
    private StyledDocument logDoc;
    private JProgressBar progressBar;
    private JSpinner     spinLimit;
    private JComboBox<StudentItem> cboStudent;
    private JLabel       statLabel;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public AnalysisPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BACKGROUND);

        add(buildConfigCard(), BorderLayout.NORTH);
        add(buildLogCard(),    BorderLayout.CENTER);
    }

    // ── Config card ───────────────────────────────────────────────────────────

    private JPanel buildConfigCard() {
        JPanel card = new JPanel(new BorderLayout(12, 10));
        card.setBackground(UIConstants.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, UIConstants.ACCENT),
                new EmptyBorder(16, 20, 16, 20)));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel title = new JLabel("⚡  Gemini AI Analysis");
        title.setFont(UIConstants.SUBHEADER);
        title.setForeground(UIConstants.PRIMARY_DARK);

        JLabel hint = new JLabel("Chọn tài khoản và số lượng bài, sau đó nhấn Run.");
        hint.setFont(UIConstants.LABEL);
        hint.setForeground(UIConstants.TEXT_MUTED);
        hint.setHorizontalAlignment(SwingConstants.RIGHT);

        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(hint,  BorderLayout.EAST);

        // Row 1: controls
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        row1.setOpaque(false);

        cboStudent = new JComboBox<>();
        cboStudent.setFont(UIConstants.MAIN);
        cboStudent.setPreferredSize(new Dimension(210, 32));
        loadStudentCombo();

        spinLimit = new JSpinner(new SpinnerNumberModel(10, 1, 200, 5));
        spinLimit.setFont(UIConstants.MAIN);
        ((JSpinner.DefaultEditor) spinLimit.getEditor()).getTextField().setColumns(5);

        row1.add(makeLabel("Tài khoản:"));
        row1.add(cboStudent);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(makeLabel("Batch size:"));
        row1.add(spinLimit);

        // Row 2: buttons + progress
        JPanel row2 = new JPanel(new BorderLayout(12, 0));
        row2.setOpaque(false);
        row2.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setOpaque(false);

        StyledButton btnStart  = new StyledButton("▶  Run AI Analysis", StyledButton.Variant.SUCCESS);
        StyledButton btnTest   = new StyledButton("🔍 Test Gemini API",  StyledButton.Variant.ACCENT);
        StyledButton btnClear  = new StyledButton("Clear Log",            StyledButton.Variant.NEUTRAL);
        StyledButton btnReload = new StyledButton("↻ Reload Accounts",   StyledButton.Variant.NEUTRAL);

        btnStart.addActionListener(e  -> startAnalysis(btnStart));
        btnTest.addActionListener(e   -> testGeminiApi(btnTest));
        btnClear.addActionListener(e  -> clearLog());
        btnReload.addActionListener(e -> loadStudentCombo());

        btnPanel.add(btnStart);
        btnPanel.add(btnTest);
        btnPanel.add(btnClear);
        btnPanel.add(btnReload);

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

    // ── Log card ──────────────────────────────────────────────────────────────

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

        // Console header bar
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
        card.setBorder(new EmptyBorder(10, 18, 14, 18));
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void loadStudentCombo() {
        cboStudent.removeAllItems();
        cboStudent.addItem(new StudentItem(0, "Tất cả tài khoản"));
        for (Student s : studentDAO.findAll()) {
            cboStudent.addItem(new StudentItem(s.getId(), s.getUsername()));
        }
    }

    /** Quick test: sends a minimal prompt to Gemini to verify key + connectivity. */
    private void testGeminiApi(JButton btn) {
        btn.setEnabled(false);
        appendLog("──────────────────────────────────────────────", LogLevel.DIVIDER);
        appendLog("[TEST] Đang kiểm tra kết nối Gemini API...", LogLevel.INFO);

        new Thread(() -> {
            try {
                String result = geminiService.generate(
                        "Reply with exactly: {\"status\":\"ok\"}");
                appendLog("[TEST] ✅ Gemini API hoạt động tốt!", LogLevel.SUCCESS);
                appendLog("[TEST] Response: " + result, LogLevel.INFO);
            } catch (Exception ex) {
                appendLog("[TEST] ❌ Gemini API lỗi: " + ex.getMessage(), LogLevel.ERROR);
                appendLog("[TEST] Nguyên nhân có thể:", LogLevel.WARN);
                appendLog("       • API key sai hoặc hết quota.", LogLevel.WARN);
                appendLog("       • Không có kết nối internet.", LogLevel.WARN);
                appendLog("       • Model name không tồn tại (kiểm tra config.properties).", LogLevel.WARN);
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
                appendLog("[INFO] Có thể do:", LogLevel.WARN);
                appendLog("       • Source code trong DB bị NULL hoặc rỗng (crawler chưa lấy được code).", LogLevel.WARN);
                appendLog("       • Tất cả bài đã được phân tích rồi.", LogLevel.WARN);
                appendLog("[INFO] Hãy thử vào tab Submissions để kiểm tra source_code.", LogLevel.INFO);
                SwingUtilities.invokeLater(() -> {
                    btnStart.setEnabled(true);
                    progressBar.setValue(0);
                    progressBar.setString("Không có bài cần phân tích.");
                    statLabel.setText("Không tìm thấy submission nào có source code.");
                });
                return;
            }

            appendLog(String.format("[INFO] Tìm thấy %d bài cần phân tích. Bắt đầu gửi lên Gemini...",
                    pending.size()), LogLevel.INFO);

            int total   = pending.size();
            int success = 0;
            int failed  = 0;

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

                // Kiểm tra source_code trước khi gửi
                if (s.getSourceCode() == null || s.getSourceCode().isBlank()) {
                    appendLog("       ⚠ Bỏ qua: source_code rỗng (crawler chưa lấy được code).", LogLevel.WARN);
                    failed++;
                    continue;
                }

                try {
                    var result = analyzer.analyzeSubmission(s);
                    if (result != null) {
                        appendLog("       ✅ Xong. AI score: " + result.getAiUsageScore()
                                + "/10  | Complexity: " + result.getComplexityEstimate(), LogLevel.SUCCESS);
                        success++;
                    } else {
                        appendLog("       ❌ Phân tích trả về null (xem log console để biết chi tiết).", LogLevel.ERROR);
                        failed++;
                    }
                } catch (Exception ex) {
                    appendLog("       ❌ Lỗi: " + ex.getMessage(), LogLevel.ERROR);
                    // In full cause chain để debug
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        appendLog("         Caused by: " + cause.getMessage(), LogLevel.ERROR);
                        cause = cause.getCause();
                    }
                    failed++;
                }

                try { 
                    // Gemini Free Tier limit: 15 Requests Per Minute (RPM)
                    // Delaying for 4.5 seconds ensures we max out at ~13 RPM, avoiding 429 Quota errors.
                    Thread.sleep(4500); 
                } catch (InterruptedException ignored) {}
            }

            final int fSuccess = success;
            final int fFailed  = failed;
            appendLog("══════════════════════════════════════════════════", LogLevel.DIVIDER);
            appendLog(String.format("[DONE] Tổng: %d bài  |  ✅ %d thành công  |  ❌ %d lỗi",
                    total, fSuccess, fFailed), fFailed > 0 ? LogLevel.WARN : LogLevel.SUCCESS);

            SwingUtilities.invokeLater(() -> {
                btnStart.setEnabled(true);
                progressBar.setValue(100);
                progressBar.setString("Hoàn tất!");
                statLabel.setText(String.format("Xong: %d/%d bài (%d lỗi)", fSuccess, total, fFailed));
            });
        }, "analysis-thread").start();
    }

    // ── Logging helpers ───────────────────────────────────────────────────────

    private enum LogLevel { INFO, SUCCESS, WARN, ERROR, DIVIDER }

    private void clearLog() {
        try {
            logDoc.remove(0, logDoc.getLength());
        } catch (BadLocationException ignored) {}
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
                    default      -> UIConstants.CONSOLE_FG;     // INFO
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
