package com.codeanalyzer.ui.panels;

import com.codeanalyzer.crawler.CodeforceCrawler;
import com.codeanalyzer.database.StudentDAO;
import com.codeanalyzer.model.PlatformType;
import com.codeanalyzer.model.Student;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StudentManagementPanel extends JPanel {

    private final StudentDAO       studentDAO = new StudentDAO();
    private       JTable           table;
    private       DefaultTableModel tableModel;
    private       JTextArea        logArea;

    public StudentManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildLogPanel(), BorderLayout.SOUTH);

        refreshData();
    }

    // ── UI builders ───────────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

        JButton btnAdd     = new JButton("➕ Thêm sinh viên");
        JButton btnRefresh = new JButton("🔄 Làm mới");
        JButton btnCrawl   = new JButton("🤖 Bắt đầu cào (Semi-Auto)");
        JButton btnDelete  = new JButton("🗑️ Xóa");

        toolbar.add(btnAdd);
        toolbar.add(btnRefresh);
        toolbar.add(new JSeparator(JSeparator.VERTICAL));
        toolbar.add(btnCrawl);
        toolbar.add(btnDelete);

        btnAdd.addActionListener(e -> addStudent());
        btnRefresh.addActionListener(e -> refreshData());
        btnCrawl.addActionListener(e -> crawlSelected());
        btnDelete.addActionListener(e -> deleteSelected());

        return toolbar;
    }

    private JScrollPane buildCenter() {
        String[] columns = {"ID", "Username", "Platform", "Thêm lúc", "Cào lần cuối", "Đang hoạt động"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        return new JScrollPane(table);
    }

    private JPanel buildLogPanel() {
        logArea = new JTextArea(6, 0);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 255, 200));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Log cào dữ liệu"));
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void addStudent() {
        String username = JOptionPane.showInputDialog(this,
                "Nhập username Codeforces:", "Thêm sinh viên", JOptionPane.PLAIN_MESSAGE);
        if (username == null || username.isBlank()) return;

        Student s = new Student(username.trim(), PlatformType.CODEFORCES);
        int id = studentDAO.save(s);
        if (id > 0) {
            appendLog("✅ Đã thêm sinh viên: " + username);
            refreshData();
        } else {
            appendLog("⚠️ Username đã tồn tại hoặc có lỗi khi lưu.");
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Chọn 1 sinh viên trước!"); return; }

        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa sinh viên \"" + name + "\" khỏi danh sách?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            studentDAO.deactivate(id);
            appendLog("🗑️ Đã ẩn sinh viên: " + name);
            refreshData();
        }
    }

    private void crawlSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 sinh viên!");
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        Student s = studentDAO.findById(id);
        if (s == null) { appendLog("Không tìm thấy sinh viên!"); return; }

        appendLog("══════════════════════════════════════");
        appendLog("Bắt đầu cào cho: " + s.getUsername());

        // Run in background thread to keep UI responsive
        new Thread(() -> {
            CodeforceCrawler crawler = new CodeforceCrawler();
            crawler.crawlForStudent(s, msg -> SwingUtilities.invokeLater(() -> appendLog(msg)));
            SwingUtilities.invokeLater(this::refreshData);
        }, "crawler-thread").start();
    }

    void refreshData() {
        tableModel.setRowCount(0);
        List<Student> students = studentDAO.findAll();
        for (Student s : students) {
            tableModel.addRow(new Object[]{
                s.getId(),
                s.getUsername(),
                s.getPlatform(),
                s.getAddedAt()  != null ? s.getAddedAt().toLocalDate()       : "-",
                s.getLastCrawledAt() != null ? s.getLastCrawledAt().toString() : "Chưa cào",
                s.isActive() ? "✅" : "❌"
            });
        }
    }

    private void appendLog(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength()); // auto-scroll
    }
}
