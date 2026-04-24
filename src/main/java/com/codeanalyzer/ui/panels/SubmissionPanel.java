package com.codeanalyzer.ui.panels;

import com.codeanalyzer.database.StudentDAO;
import com.codeanalyzer.database.SubmissionDAO;
import com.codeanalyzer.model.Student;
import com.codeanalyzer.model.Submission;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SubmissionPanel extends JPanel {

    private final SubmissionDAO submissionDAO = new SubmissionDAO();
    private final StudentDAO    studentDAO    = new StudentDAO();
    private DefaultTableModel tableModel;
    private JComboBox<StudentItem> cboStudent;
    private JTextArea sourceArea;

    public SubmissionPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildSplitPane(), BorderLayout.CENTER);

        loadStudentCombo();
        refreshData();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

        cboStudent = new JComboBox<>();
        cboStudent.setPreferredSize(new Dimension(200, 28));
        bar.add(new JLabel("Sinh viên:"));
        bar.add(cboStudent);

        JButton btnRefresh = new JButton("🔄 Làm mới");
        btnRefresh.addActionListener(e -> refreshData());
        bar.add(btnRefresh);

        return bar;
    }

    // ── Split: table (left) + source preview (right) ─────────────────────────
    private JSplitPane buildSplitPane() {
        // Table
        String[] cols = {"ID", "Username", "Problem ID", "Tên bài", "Ngôn ngữ", "Verdict", "Thời gian nộp"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSourceCode(table.getSelectedRow());
        });

        // Source preview
        sourceArea = new JTextArea();
        sourceArea.setEditable(false);
        sourceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        sourceArea.setTabSize(4);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Source Code"));
        rightPanel.add(new JScrollPane(sourceArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(table), rightPanel);
        split.setResizeWeight(0.5);
        return split;
    }

    // ── Data ──────────────────────────────────────────────────────────────────
    private void loadStudentCombo() {
        cboStudent.removeAllItems();
        cboStudent.addItem(new StudentItem(0, "-- Tất cả --"));
        for (Student s : studentDAO.findAll()) {
            cboStudent.addItem(new StudentItem(s.getId(), s.getUsername()));
        }
    }

    private void refreshData() {
        tableModel.setRowCount(0);
        sourceArea.setText("");

        StudentItem selected = (StudentItem) cboStudent.getSelectedItem();
        List<Submission> list;
        if (selected == null || selected.id == 0) {
            list = submissionDAO.findAll(200);
        } else {
            list = submissionDAO.findByStudentId(selected.id);
        }

        for (Submission s : list) {
            tableModel.addRow(new Object[]{
                s.getId(),
                s.getStudentUsername(),
                s.getProblemId(),
                s.getProblemName(),
                s.getLanguage(),
                s.getVerdict(),
                s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : "-"
            });
        }
    }

    private void showSourceCode(int row) {
        if (row < 0) { sourceArea.setText(""); return; }
        // fetch full object to get source_code (not shown in table to save memory)
        int id = (int) tableModel.getValueAt(row, 0);
        // We stored submissions already but without source in table; re-query by ID
        // Quick approach: just show what we have from last findAll
        // Better: do a direct query. For now use a lazy findAll approach:
        StudentItem sel = (StudentItem) cboStudent.getSelectedItem();
        List<Submission> list = (sel == null || sel.id == 0)
            ? submissionDAO.findAll(200)
            : submissionDAO.findByStudentId(sel.id);

        list.stream().filter(s -> s.getId() == id).findFirst().ifPresent(s -> {
            String code = s.getSourceCode();
            sourceArea.setText(code != null ? code : "(Không có source code)");
            sourceArea.setCaretPosition(0);
        });
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private record StudentItem(int id, String name) {
        @Override public String toString() { return name; }
    }
}
