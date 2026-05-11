package com.codeanalyzer.ui.panels;

import com.codeanalyzer.database.StudentDAO;
import com.codeanalyzer.database.SubmissionDAO;
import com.codeanalyzer.model.Student;
import com.codeanalyzer.model.Submission;
import com.codeanalyzer.ui.UIConstants;
import com.codeanalyzer.ui.components.StyledButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel displaying crawled submissions with a source code preview.
 * Refined UI with modern table styles and rounded corners.
 */
public class SubmissionPanel extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm   dd/MM/yyyy");

    private final SubmissionDAO         submissionDAO = new SubmissionDAO();
    private final StudentDAO            studentDAO    = new StudentDAO();
    private       DefaultTableModel     tableModel;
    private       JComboBox<StudentItem> cboStudent;
    private       JTextArea             sourceArea;
    private       JLabel                sourceTitleLbl;
    private       List<Submission>      currentList;
    private       boolean               isUpdatingCombo = false;

    public SubmissionPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BACKGROUND);

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildSplitPane(), BorderLayout.CENTER);

        loadStudentCombo();
        refreshData();
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UIConstants.CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER),
                new EmptyBorder(12, 20, 12, 20)));

        JLabel title = new JLabel("📄 Submissions Data");
        title.setFont(UIConstants.SUBHEADER);
        title.setForeground(UIConstants.PRIMARY_DARK);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JLabel lbl = new JLabel("Filter Account:");
        lbl.setFont(UIConstants.SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);

        cboStudent = new JComboBox<>();
        cboStudent.setFont(UIConstants.MAIN);
        cboStudent.setPreferredSize(new Dimension(200, 32));
        cboStudent.addActionListener(e -> {
            if (!isUpdatingCombo) refreshData();
        });

        cboStudent.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                loadStudentCombo();
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });

        StyledButton btnRefresh = new StyledButton("Refresh", StyledButton.Variant.NEUTRAL);
        btnRefresh.addActionListener(e -> refreshData());

        right.add(lbl);
        right.add(cboStudent);
        right.add(Box.createHorizontalStrut(10));
        right.add(btnRefresh);

        bar.add(title, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Main split ────────────────────────────────────────────────────────────

    private JSplitPane buildSplitPane() {
        // ── LEFT: submissions table ──
        String[] cols = {"ID", "Username", "Problem", "Language", "Verdict", "Submitted Time"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? UIConstants.CARD_BG : UIConstants.TABLE_ALT_ROW);
                    c.setForeground(UIConstants.TEXT);
                } else {
                    c.setBackground(UIConstants.TABLE_SEL_BG);
                    c.setForeground(UIConstants.TABLE_SEL_FG);
                }
                
                // Tô màu Verdict
                if (col == 4 && !isRowSelected(row)) {
                    String verdict = (String) getValueAt(row, col);
                    if ("Accepted".equalsIgnoreCase(verdict) || "OK".equalsIgnoreCase(verdict)) {
                        c.setForeground(new Color(0x059669)); // Emerald 600
                        c.setFont(UIConstants.SMALL_BOLD);
                    } else if ("Wrong Answer".equalsIgnoreCase(verdict)) {
                        c.setForeground(new Color(0xDC2626)); // Red 600
                        c.setFont(UIConstants.MAIN);
                    } else if (verdict != null && verdict.contains("Time Limit")) {
                        c.setForeground(new Color(0xD97706)); // Amber 600
                    }
                } else if (col != 4) {
                    c.setFont(UIConstants.MAIN);
                }
                return c;
            }
        };

        styleTable(table);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(3).setMaxWidth(130);
        table.getColumnModel().getColumn(4).setMaxWidth(120);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSourceCode(table.getSelectedRow());
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(UIConstants.CARD_BG);

        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.setBackground(UIConstants.BACKGROUND);
        leftWrap.add(tableScroll, BorderLayout.CENTER);

        // ── RIGHT: source code viewer ──
        sourceArea = new JTextArea("Vui lòng chọn một bài nộp bên trái để xem source code.");
        sourceArea.setEditable(false);
        sourceArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        sourceArea.setBackground(UIConstants.CONSOLE_BG);
        sourceArea.setForeground(new Color(0xE2E8F0));
        sourceArea.setCaretColor(UIConstants.ACCENT_LIGHT);
        sourceArea.setBorder(new EmptyBorder(12, 16, 12, 16));
        sourceArea.setTabSize(4);

        JScrollPane sourceScroll = new JScrollPane(sourceArea);
        sourceScroll.setBorder(BorderFactory.createEmptyBorder());
        sourceScroll.getViewport().setBackground(UIConstants.CONSOLE_BG);

        sourceTitleLbl = new JLabel("  </>  Source Code Preview");
        sourceTitleLbl.setFont(UIConstants.SMALL_BOLD);
        sourceTitleLbl.setForeground(new Color(0x80CBC4)); // Soft Teal
        sourceTitleLbl.setBackground(new Color(0x0D1228));
        sourceTitleLbl.setOpaque(true);
        sourceTitleLbl.setPreferredSize(new Dimension(0, 32));
        sourceTitleLbl.setBorder(new EmptyBorder(0, 8, 0, 0));

        JPanel rightInner = new JPanel(new BorderLayout());
        rightInner.setBackground(UIConstants.CONSOLE_BG);
        rightInner.add(sourceTitleLbl, BorderLayout.NORTH);
        rightInner.add(sourceScroll, BorderLayout.CENTER);
        rightInner.setBorder(BorderFactory.createLineBorder(new Color(0x1A2A4A)));

        // Bọc lại để có padding
        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setOpaque(false);
        rightWrap.setBorder(new EmptyBorder(0, 8, 0, 0));
        rightWrap.add(rightInner, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftWrap, rightWrap);
        split.setResizeWeight(0.55); // 55% bảng, 45% code
        split.setDividerSize(6);
        split.setBorder(new EmptyBorder(12, 16, 16, 16));
        split.setBackground(UIConstants.BACKGROUND);
        return split;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadStudentCombo() {
        isUpdatingCombo = true;
        try {
            Object selObj = cboStudent.getSelectedItem();
            int selId = (selObj instanceof StudentItem) ? ((StudentItem) selObj).id() : 0;

            cboStudent.removeAllItems();
            StudentItem all = new StudentItem(0, "All accounts");
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
        } finally {
            isUpdatingCombo = false;
        }
    }

    private void refreshData() {
        tableModel.setRowCount(0);
        sourceArea.setText("Vui lòng chọn một bài nộp bên trái để xem source code.");
        sourceTitleLbl.setText("  </>  Source Code Preview");

        StudentItem sel = (StudentItem) cboStudent.getSelectedItem();
        currentList = (sel == null || sel.id == 0)
                ? submissionDAO.findAll(300)
                : submissionDAO.findByStudentId(sel.id);

        for (Submission s : currentList) {
            tableModel.addRow(new Object[]{
                s.getId(),
                s.getStudentUsername(),
                s.getProblemName(),
                s.getLanguage(),
                s.getVerdict(),
                s.getSubmittedAt() != null ? s.getSubmittedAt().format(DT_FMT) : "-"
            });
        }
    }

    private void showSourceCode(int row) {
        if (row < 0 || currentList == null) {
            sourceArea.setText("");
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        currentList.stream().filter(s -> s.getId() == id).findFirst().ifPresent(s -> {
            String code = s.getSourceCode();
            sourceArea.setText(code != null ? code : "(Không lấy được mã nguồn)");
            sourceArea.setCaretPosition(0);
            sourceTitleLbl.setText("  </>  " + s.getProblemName() + "  —  " + s.getStudentUsername()
                    + "  [" + s.getLanguage() + "]");
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void styleTable(JTable t) {
        t.setRowHeight(32); // Cao hơn tí cho thoáng
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
        t.getTableHeader().setPreferredSize(new Dimension(0, 36));
        t.getTableHeader().setReorderingAllowed(false);
    }

    private record StudentItem(int id, String name) {
        @Override public String toString() { return name; }
    }
}
