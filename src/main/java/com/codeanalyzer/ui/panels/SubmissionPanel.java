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
 */
public class SubmissionPanel extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SubmissionDAO         submissionDAO = new SubmissionDAO();
    private final StudentDAO            studentDAO    = new StudentDAO();
    private       DefaultTableModel     tableModel;
    private       JComboBox<StudentItem> cboStudent;
    private       JTextArea             sourceArea;
    private       JLabel                sourceTitleLbl;
    private       List<Submission>      currentList;

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
                new EmptyBorder(10, 16, 10, 16)));

        JLabel title = new JLabel("Submissions");
        title.setFont(UIConstants.SUBHEADER);
        title.setForeground(UIConstants.PRIMARY_DARK);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JLabel lbl = new JLabel("Filter:");
        lbl.setFont(UIConstants.SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);

        cboStudent = new JComboBox<>();
        cboStudent.setFont(UIConstants.MAIN);
        cboStudent.setPreferredSize(new Dimension(200, 30));
        cboStudent.addActionListener(e -> refreshData());

        StyledButton btnRefresh = new StyledButton("Refresh", StyledButton.Variant.NEUTRAL);
        btnRefresh.addActionListener(e -> refreshData());

        right.add(lbl);
        right.add(cboStudent);
        right.add(btnRefresh);

        bar.add(title, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Main split ────────────────────────────────────────────────────────────

    private JSplitPane buildSplitPane() {
        // Left: submissions table
        String[] cols = {"ID", "Username", "Problem", "Language", "Verdict", "Submitted"};
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
                return c;
            }
        };

        styleTable(table);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(0).setMaxWidth(55);
        table.getColumnModel().getColumn(3).setMaxWidth(130);
        table.getColumnModel().getColumn(4).setMaxWidth(90);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSourceCode(table.getSelectedRow());
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(UIConstants.CARD_BG);

        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.setBackground(UIConstants.BACKGROUND);
        leftWrap.add(tableScroll, BorderLayout.CENTER);

        // Right: source code viewer
        sourceArea = new JTextArea("Select a submission to view its source code.");
        sourceArea.setEditable(false);
        sourceArea.setFont(UIConstants.MONO);
        sourceArea.setBackground(UIConstants.CONSOLE_BG);
        sourceArea.setForeground(new Color(0xCFD8DC));
        sourceArea.setCaretColor(UIConstants.ACCENT_LIGHT);
        sourceArea.setBorder(new EmptyBorder(10, 14, 10, 14));
        sourceArea.setTabSize(4);

        JScrollPane sourceScroll = new JScrollPane(sourceArea);
        sourceScroll.setBorder(BorderFactory.createEmptyBorder());

        sourceTitleLbl = new JLabel("  Source Code Preview");
        sourceTitleLbl.setFont(UIConstants.SMALL_BOLD);
        sourceTitleLbl.setForeground(new Color(0x80CBC4));
        sourceTitleLbl.setBackground(new Color(0x0D1228));
        sourceTitleLbl.setOpaque(true);
        sourceTitleLbl.setPreferredSize(new Dimension(0, 26));

        JPanel rightInner = new JPanel(new BorderLayout());
        rightInner.setBackground(UIConstants.CONSOLE_BG);
        rightInner.add(sourceTitleLbl, BorderLayout.NORTH);
        rightInner.add(sourceScroll, BorderLayout.CENTER);
        rightInner.setBorder(BorderFactory.createLineBorder(new Color(0x1A2A4A)));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftWrap, rightInner);
        split.setResizeWeight(0.45);
        split.setDividerSize(5);
        split.setBorder(new EmptyBorder(8, 16, 12, 16));
        split.setBackground(UIConstants.BACKGROUND);
        return split;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadStudentCombo() {
        cboStudent.removeAllItems();
        cboStudent.addItem(new StudentItem(0, "All accounts"));
        for (Student s : studentDAO.findAll()) {
            cboStudent.addItem(new StudentItem(s.getId(), s.getUsername()));
        }
    }

    private void refreshData() {
        tableModel.setRowCount(0);
        sourceArea.setText("Select a submission to view its source code.");
        sourceTitleLbl.setText("  Source Code Preview");

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
            sourceArea.setText(code != null ? code : "(No source code available)");
            sourceArea.setCaretPosition(0);
            sourceTitleLbl.setText("  " + s.getProblemName() + "  —  " + s.getStudentUsername()
                    + "  [" + s.getLanguage() + "]");
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
        t.getTableHeader().setPreferredSize(new Dimension(0, 34));
        t.getTableHeader().setReorderingAllowed(false);
    }

    private record StudentItem(int id, String name) {
        @Override public String toString() { return name; }
    }
}
