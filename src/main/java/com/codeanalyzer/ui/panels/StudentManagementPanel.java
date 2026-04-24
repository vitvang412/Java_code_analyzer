package com.codeanalyzer.ui.panels;

import com.codeanalyzer.crawler.CodeforceCrawler;
import com.codeanalyzer.database.StudentDAO;
import com.codeanalyzer.model.PlatformType;
import com.codeanalyzer.model.Student;
import com.codeanalyzer.ui.UIConstants;
import com.codeanalyzer.ui.components.StyledButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudentManagementPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StudentDAO        studentDAO = new StudentDAO();
    private       JTable            table;
    private       DefaultTableModel tableModel;
    private       JTextArea         logArea;

    public StudentManagementPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BACKGROUND);

        add(buildTopBar(),   BorderLayout.NORTH);
        add(buildCenter(),   BorderLayout.CENTER);
        add(buildLogPanel(), BorderLayout.SOUTH);

        refreshData();
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UIConstants.CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER),
                new EmptyBorder(10, 16, 10, 16)));

        JLabel title = new JLabel("Manage Student Accounts");
        title.setFont(UIConstants.SUBHEADER);
        title.setForeground(UIConstants.PRIMARY_DARK);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        StyledButton btnAdd     = new StyledButton("Add Account",   StyledButton.Variant.PRIMARY);
        StyledButton btnCrawl   = new StyledButton("Start Crawl",   StyledButton.Variant.ACCENT);
        StyledButton btnRefresh = new StyledButton("Refresh",       StyledButton.Variant.NEUTRAL);
        StyledButton btnDelete  = new StyledButton("Remove",        StyledButton.Variant.DANGER);

        btnAdd.addActionListener(e -> addStudent());
        btnCrawl.addActionListener(e -> crawlSelected());
        btnRefresh.addActionListener(e -> refreshData());
        btnDelete.addActionListener(e -> deleteSelected());

        buttons.add(btnRefresh);
        buttons.add(btnAdd);
        buttons.add(btnDelete);
        buttons.add(btnCrawl);

        bar.add(title,   BorderLayout.WEST);
        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildCenter() {
        String[] columns = {"ID", "Username", "Platform", "Date Added", "Last Crawled", "Active"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? UIConstants.CARD_BG : UIConstants.TABLE_ALT_ROW);
                } else {
                    c.setBackground(UIConstants.TABLE_SEL_BG);
                    c.setForeground(UIConstants.TABLE_SEL_FG);
                }
                return c;
            }
        };

        styleTable(table);

        // Active column renderer
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(new ActiveRenderer());

        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(2).setMaxWidth(120);
        table.getColumnModel().getColumn(5).setMaxWidth(70);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(UIConstants.CARD_BG);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(UIConstants.BACKGROUND);
        wrap.setBorder(new EmptyBorder(12, 16, 6, 16));
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildLogPanel() {
        logArea = new JTextArea(7, 0);
        logArea.setEditable(false);
        logArea.setFont(UIConstants.MONO);
        logArea.setBackground(UIConstants.CONSOLE_BG);
        logArea.setForeground(UIConstants.CONSOLE_FG);
        logArea.setCaretColor(UIConstants.ACCENT_LIGHT);
        logArea.setBorder(new EmptyBorder(6, 10, 6, 10));

        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createEmptyBorder());

        JLabel lbl = new JLabel("  Crawl Log");
        lbl.setFont(UIConstants.SMALL_BOLD);
        lbl.setForeground(UIConstants.CONSOLE_FG);
        lbl.setBackground(new Color(0x0D1228));
        lbl.setOpaque(true);
        lbl.setPreferredSize(new Dimension(0, 26));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(UIConstants.CONSOLE_BG);
        wrap.setBorder(new EmptyBorder(0, 16, 12, 16));

        JPanel inner = new JPanel(new BorderLayout());
        inner.add(lbl, BorderLayout.NORTH);
        inner.add(sp,  BorderLayout.CENTER);
        inner.setBorder(BorderFactory.createLineBorder(new Color(0x1A2A4A)));

        wrap.add(inner, BorderLayout.CENTER);
        return wrap;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void addStudent() {
        JTextField field = new JTextField(20);
        field.setFont(UIConstants.MAIN);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.add(new JLabel("Codeforces username:"), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);

        int res = JOptionPane.showConfirmDialog(this, panel, "Add Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String username = field.getText().trim();
        if (username.isEmpty()) return;

        Student s = new Student(username, PlatformType.CODEFORCES);
        int id = studentDAO.save(s);
        if (id > 0) {
            appendLog("[OK] Added: " + username);
            refreshData();
        } else {
            appendLog("[WARN] Username already exists or save failed.");
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            showWarn("Please select an account first.");
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove \"" + name + "\" from the list?",
                "Confirm Remove", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            studentDAO.deactivate(id);
            appendLog("[OK] Removed: " + name);
            refreshData();
        }
    }

    private void crawlSelected() {
        int row = table.getSelectedRow();
        if (row == -1) { showWarn("Please select an account first."); return; }

        int id = (int) tableModel.getValueAt(row, 0);
        Student s = studentDAO.findById(id);
        if (s == null) { appendLog("[ERR] Student not found."); return; }

        appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        appendLog("[START] Crawling: " + s.getUsername());

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
                s.getAddedAt() != null ? s.getAddedAt().format(DATE_FMT) : "-",
                s.getLastCrawledAt() != null ? s.getLastCrawledAt().format(DATE_FMT) : "Never",
                s.isActive()
            });
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void showWarn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.WARNING_MESSAGE);
    }

    private void styleTable(JTable t) {
        t.setRowHeight(30);
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

    /** Renders boolean active status as colored pill. */
    private static class ActiveRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel lbl = new JLabel();
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(true);
            boolean active = Boolean.TRUE.equals(v);
            lbl.setText(active ? "Active" : "Inactive");
            if (sel) {
                lbl.setBackground(UIConstants.TABLE_SEL_BG);
                lbl.setForeground(active ? UIConstants.SUCCESS : UIConstants.DANGER);
            } else {
                lbl.setBackground(active ? UIConstants.SUCCESS_LIGHT : UIConstants.DANGER_LIGHT);
                lbl.setForeground(active ? UIConstants.SUCCESS : UIConstants.DANGER);
            }
            lbl.setFont(UIConstants.SMALL_BOLD);
            return lbl;
        }
    }
}
