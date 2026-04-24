package com.codeanalyzer.ui.panels;

import com.codeanalyzer.ai.CodeAnalyzer;
import com.codeanalyzer.database.StudentDAO;
import com.codeanalyzer.database.SubmissionDAO;
import com.codeanalyzer.model.Student;
import com.codeanalyzer.model.Submission;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AnalysisPanel extends JPanel {

    private final SubmissionDAO submissionDAO = new SubmissionDAO();
    private final StudentDAO    studentDAO    = new StudentDAO();
    private final CodeAnalyzer  analyzer      = new CodeAnalyzer();

    private JTextArea logArea;
    private JProgressBar progressBar;
    private JSpinner spinLimit;
    private JComboBox<StudentItem> cboStudent;

    public AnalysisPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildTopPanel(),  BorderLayout.NORTH);
        add(buildLogPanel(),  BorderLayout.CENTER);
    }

    // ── Top controls ──────────────────────────────────────────────────────────
    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new BorderLayout(6, 6));

        // Config row
        JPanel configRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        cboStudent = new JComboBox<>();
        cboStudent.setPreferredSize(new Dimension(180, 28));
        loadStudentCombo();
        configRow.add(new JLabel("Sinh viên:"));
        configRow.add(cboStudent);

        spinLimit = new JSpinner(new SpinnerNumberModel(10, 1, 200, 5));
        configRow.add(new JLabel("  Số bài mỗi đợt:"));
        configRow.add(spinLimit);

        top.add(configRow, BorderLayout.NORTH);

        // Button row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnStart  = new JButton("▶ Bắt đầu phân tích (Gemini AI)");
        JButton btnClear  = new JButton("🗑 Xóa log");
        btnStart.setBackground(new Color(34, 139, 34));
        btnStart.setForeground(Color.WHITE);
        btnStart.setFont(btnStart.getFont().deriveFont(Font.BOLD));

        btnStart.addActionListener(e -> startAnalysis(btnStart));
        btnClear.addActionListener(e -> logArea.setText(""));

        btnRow.add(btnStart);
        btnRow.add(btnClear);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Chờ...");
        btnRow.add(progressBar);

        top.add(btnRow, BorderLayout.CENTER);
        return top;
    }

    private JPanel buildLogPanel() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(new Color(20, 20, 30));
        logArea.setForeground(new Color(200, 230, 200));
        logArea.setCaretColor(Color.WHITE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Log phân tích Gemini AI"));
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────
    private void loadStudentCombo() {
        cboStudent.removeAllItems();
        cboStudent.addItem(new StudentItem(0, "-- Tất cả --"));
        for (Student s : studentDAO.findAll()) {
            cboStudent.addItem(new StudentItem(s.getId(), s.getUsername()));
        }
    }

    private void startAnalysis(JButton btnStart) {
        btnStart.setEnabled(false);
        int limit = (int) spinLimit.getValue();

        new Thread(() -> {
            List<Submission> unanalyzed;

            StudentItem sel = (StudentItem) cboStudent.getSelectedItem();
            if (sel != null && sel.id != 0) {
                // Filter by student: get unanalyzed for that student only
                unanalyzed = submissionDAO.findUnanalyzedByStudent(sel.id, limit);
            } else {
                unanalyzed = submissionDAO.findUnanalyzed(limit);
            }

            if (unanalyzed.isEmpty()) {
                appendLog("✅ Không có bài nào cần phân tích thêm.");
                SwingUtilities.invokeLater(() -> {
                    btnStart.setEnabled(true);
                    progressBar.setValue(0);
                    progressBar.setString("Không còn bài cần phân tích.");
                });
                return;
            }

            appendLog("══════════════════════════════════════════");
            appendLog("Bắt đầu phân tích " + unanalyzed.size() + " bài nộp bằng Gemini AI...");

            int total = unanalyzed.size();
            for (int i = 0; i < total; i++) {
                Submission s = unanalyzed.get(i);
                int finalI = i;
                SwingUtilities.invokeLater(() -> {
                    int pct = (int) (100.0 * finalI / total);
                    progressBar.setValue(pct);
                    progressBar.setString(String.format("%d/%d bài (%d%%)", finalI + 1, total, pct));
                });

                appendLog(String.format("\n[%d/%d] Phân tích: \"%s\" của %s",
                        i + 1, total, s.getProblemName(), s.getStudentUsername()));

                try {
                    analyzer.analyzeSubmission(s);
                    appendLog("  → ✅ Hoàn tất.");
                } catch (Exception ex) {
                    appendLog("  → ❌ Lỗi: " + ex.getMessage());
                }

                // Small delay to avoid rate-limiting
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }

            appendLog("\n══════════════════════════════════════════");
            appendLog("🎉 Phân tích xong " + total + " bài!");
            SwingUtilities.invokeLater(() -> {
                btnStart.setEnabled(true);
                progressBar.setValue(100);
                progressBar.setString("Hoàn thành!");
            });
        }, "analysis-thread").start();
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private record StudentItem(int id, String name) {
        @Override public String toString() { return name; }
    }
}
