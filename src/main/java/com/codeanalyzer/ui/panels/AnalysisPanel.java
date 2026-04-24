package com.codeanalyzer.ui.panels;

import com.codeanalyzer.ai.CodeAnalyzer;
import com.codeanalyzer.database.StudentDAO;
import com.codeanalyzer.database.SubmissionDAO;
import com.codeanalyzer.model.Student;
import com.codeanalyzer.model.Submission;
import com.codeanalyzer.ui.UIConstants;
import com.codeanalyzer.ui.components.StyledButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Panel for triggering Gemini AI analysis on unanalyzed submissions.
 * Shows real-time progress and a console log.
 */
public class AnalysisPanel extends JPanel {

    private final SubmissionDAO submissionDAO = new SubmissionDAO();
    private final StudentDAO    studentDAO    = new StudentDAO();
    private final CodeAnalyzer  analyzer      = new CodeAnalyzer();

    private JTextArea    logArea;
    private JProgressBar progressBar;
    private JSpinner     spinLimit;
    private JComboBox<StudentItem> cboStudent;
    private JLabel       statLabel;

    public AnalysisPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BACKGROUND);

        add(buildConfigCard(), BorderLayout.NORTH);
        add(buildLogCard(),    BorderLayout.CENTER);
    }

    // ── Config card ───────────────────────────────────────────────────────────

    private JPanel buildConfigCard() {
        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setBackground(UIConstants.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER),
                new EmptyBorder(14, 18, 14, 18)));

        JLabel title = new JLabel("Gemini AI Analysis");
        title.setFont(UIConstants.SUBHEADER);
        title.setForeground(UIConstants.PRIMARY_DARK);

        // Row 1: controls
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row1.setOpaque(false);

        cboStudent = new JComboBox<>();
        cboStudent.setFont(UIConstants.MAIN);
        cboStudent.setPreferredSize(new Dimension(200, 30));
        loadStudentCombo();

        spinLimit = new JSpinner(new SpinnerNumberModel(10, 1, 200, 5));
        spinLimit.setFont(UIConstants.MAIN);
        ((JSpinner.DefaultEditor) spinLimit.getEditor()).getTextField().setColumns(5);

        row1.add(makeLabel("Account:"));
        row1.add(cboStudent);
        row1.add(makeLabel("Batch size:"));
        row1.add(spinLimit);

        // Row 2: buttons + progress
        JPanel row2 = new JPanel(new BorderLayout(12, 0));
        row2.setOpaque(false);
        row2.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setOpaque(false);

        StyledButton btnStart  = new StyledButton("Run AI Analysis", StyledButton.Variant.SUCCESS);
        StyledButton btnClear  = new StyledButton("Clear Log",       StyledButton.Variant.NEUTRAL);
        StyledButton btnReload = new StyledButton("Reload Accounts", StyledButton.Variant.NEUTRAL);

        btnStart.addActionListener(e  -> startAnalysis(btnStart));
        btnClear.addActionListener(e  -> logArea.setText(""));
        btnReload.addActionListener(e -> loadStudentCombo());

        btnPanel.add(btnStart);
        btnPanel.add(btnReload);
        btnPanel.add(btnClear);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Idle");
        progressBar.setFont(UIConstants.SMALL);
        progressBar.setForeground(UIConstants.ACCENT);
        progressBar.setBackground(UIConstants.BACKGROUND);
        progressBar.setPreferredSize(new Dimension(0, 20));

        statLabel = new JLabel("No analysis running.");
        statLabel.setFont(UIConstants.SMALL);
        statLabel.setForeground(UIConstants.TEXT_SECONDARY);

        JPanel progressPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        progressPanel.setOpaque(false);
        progressPanel.add(progressBar);
        progressPanel.add(statLabel);

        row2.add(btnPanel,     BorderLayout.WEST);
        row2.add(progressPanel, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 4));
        content.setOpaque(false);
        content.add(row1, BorderLayout.NORTH);
        content.add(row2, BorderLayout.SOUTH);

        card.add(title,   BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ── Log card ──────────────────────────────────────────────────────────────

    private JPanel buildLogCard() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(UIConstants.MONO);
        logArea.setBackground(UIConstants.CONSOLE_BG);
        logArea.setForeground(UIConstants.CONSOLE_FG);
        logArea.setCaretColor(UIConstants.ACCENT_LIGHT);
        logArea.setBorder(new EmptyBorder(8, 12, 8, 12));

        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(UIConstants.CONSOLE_BG);

        JLabel header = new JLabel("  Analysis Output");
        header.setFont(UIConstants.SMALL_BOLD);
        header.setForeground(UIConstants.CONSOLE_FG);
        header.setBackground(new Color(0x0D1228));
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(0, 26));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UIConstants.CONSOLE_BG);
        card.setBorder(new EmptyBorder(8, 16, 12, 16));

        JPanel inner = new JPanel(new BorderLayout());
        inner.add(header, BorderLayout.NORTH);
        inner.add(sp, BorderLayout.CENTER);
        inner.setBorder(BorderFactory.createLineBorder(new Color(0x1A2A4A)));

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void loadStudentCombo() {
        cboStudent.removeAllItems();
        cboStudent.addItem(new StudentItem(0, "All accounts"));
        for (Student s : studentDAO.findAll()) {
            cboStudent.addItem(new StudentItem(s.getId(), s.getUsername()));
        }
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

            if (pending.isEmpty()) {
                appendLog("[INFO] No submissions pending analysis.");
                SwingUtilities.invokeLater(() -> {
                    btnStart.setEnabled(true);
                    progressBar.setValue(0);
                    progressBar.setString("Nothing to analyze.");
                    statLabel.setText("All submissions have been analyzed.");
                });
                return;
            }

            appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            appendLog("[START] Sending " + pending.size() + " submission(s) to Gemini AI...");

            int total = pending.size();
            for (int i = 0; i < total; i++) {
                Submission s = pending.get(i);
                final int idx = i;

                SwingUtilities.invokeLater(() -> {
                    int pct = (int) (100.0 * idx / total);
                    progressBar.setValue(pct);
                    progressBar.setString(String.format("%d / %d  (%d%%)", idx + 1, total, pct));
                    statLabel.setText("Analyzing: " + s.getProblemName() + " — " + s.getStudentUsername());
                });

                appendLog(String.format("[%d/%d] %s  (User: %s)",
                        i + 1, total, s.getProblemName(), s.getStudentUsername()));

                try {
                    analyzer.analyzeSubmission(s);
                    appendLog("  -> Done.");
                } catch (Exception ex) {
                    appendLog("  -> ERROR: " + ex.getMessage());
                }

                try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            }

            appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            appendLog("[DONE] Analysis complete for " + total + " submissions.");
            SwingUtilities.invokeLater(() -> {
                btnStart.setEnabled(true);
                progressBar.setValue(100);
                progressBar.setString("Complete!");
                statLabel.setText("Finished analyzing " + total + " submissions.");
            });
        }, "analysis-thread").start();
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
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
