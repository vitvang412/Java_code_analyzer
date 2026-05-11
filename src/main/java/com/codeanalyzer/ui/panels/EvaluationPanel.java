package com.codeanalyzer.ui.panels;

import com.codeanalyzer.ai.StudentEvaluator;
import com.codeanalyzer.database.AnalysisResultDAO;
import com.codeanalyzer.database.StudentEvaluationDAO;
import com.codeanalyzer.model.AnalysisResult;
import com.codeanalyzer.model.StudentEvaluation;
import com.codeanalyzer.ui.UIConstants;
import com.codeanalyzer.ui.components.StyledButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel showing the aggregated student evaluation results.
 * - Table: Username, Analyzed, DSA Score, AI Dependency, Top Algorithms, Top
 * DS, Grade, Updated
 * - Double-click row -> opens a detail dialog.
 */
public class EvaluationPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy");

    private final StudentEvaluationDAO evalDAO = new StudentEvaluationDAO();
    private final AnalysisResultDAO analysisDAO = new AnalysisResultDAO();
    private final StudentEvaluator evaluator = new StudentEvaluator();

    private final DefaultTableModel tableModel;
    private final JTable table;

    public EvaluationPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BACKGROUND);

        String[] cols = {
                "Username", "Analyzed", "DSA Score", "AI Dependency",
                "Top Algorithms", "Top Data Structures", "Grade", "Last Updated"
        };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
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

        TableColumnModel tcm = table.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(120);
        tcm.getColumn(1).setMaxWidth(80);
        tcm.getColumn(2).setMaxWidth(100);
        tcm.getColumn(3).setMaxWidth(110);
        tcm.getColumn(6).setMaxWidth(100);
        tcm.getColumn(7).setPreferredWidth(140);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        tcm.getColumn(1).setCellRenderer(center);
        tcm.getColumn(2).setCellRenderer(new ScoreRenderer(true));
        tcm.getColumn(3).setCellRenderer(new ScoreRenderer(false));
        tcm.getColumn(6).setCellRenderer(new GradeRenderer());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2)
                    showDetailForSelected();
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(UIConstants.CARD_BG);

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setBackground(UIConstants.BACKGROUND);
        tableWrap.setBorder(new EmptyBorder(8, 16, 12, 16));
        tableWrap.add(sp, BorderLayout.CENTER);

        add(buildTopBar(), BorderLayout.NORTH);
        add(tableWrap, BorderLayout.CENTER);

        refreshData();
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UIConstants.CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER),
                new EmptyBorder(10, 16, 10, 16)));

        JLabel title = new JLabel("Student Evaluations");
        title.setFont(UIConstants.SUBHEADER);
        title.setForeground(UIConstants.PRIMARY_DARK);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        StyledButton btnRefresh = new StyledButton("Refresh", StyledButton.Variant.NEUTRAL);
        StyledButton btnDetail = new StyledButton("View Detail", StyledButton.Variant.ACCENT);
        StyledButton btnRecalc = new StyledButton("Re-evaluate All", StyledButton.Variant.PRIMARY);

        btnRefresh.addActionListener(e -> refreshData());
        btnDetail.addActionListener(e -> showDetailForSelected());
        btnRecalc.addActionListener(e -> recalcAll(btnRecalc));

        buttons.add(btnRefresh);
        buttons.add(btnDetail);
        buttons.add(btnRecalc);

        bar.add(title, BorderLayout.WEST);
        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void refreshData() {
        tableModel.setRowCount(0);
        for (StudentEvaluation e : evalDAO.findAll()) {
            tableModel.addRow(new Object[] {
                    e.getStudentUsername(),
                    e.getTotalAnalyzed(),
                    String.format("%.2f", e.getDsaScore()),
                    String.format("%.2f", e.getAiDependencyScore()),
                    e.getTopAlgorithms(),
                    e.getTopDataStructures(),
                    e.overallLabel(),
                    e.getEvaluatedAt() == null ? "-" : e.getEvaluatedAt().format(FMT)
            });
        }
    }

    private void recalcAll(JButton src) {
        src.setEnabled(false);
        new Thread(() -> {
            try {
                List<StudentEvaluation> out = evaluator.evaluateAll();
                SwingUtilities.invokeLater(() -> {
                    refreshData();
                    JOptionPane.showMessageDialog(this,
                            "Re-evaluated " + out.size() + " student(s).",
                            "Done", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> src.setEnabled(true));
            }
        }, "evaluator-thread").start();
    }

    private void showDetailForSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.");
            return;
        }

        String username = (String) tableModel.getValueAt(row, 0);
        StudentEvaluation ev = evalDAO.findAll().stream()
                .filter(e -> username.equals(e.getStudentUsername()))
                .findFirst().orElse(null);
        if (ev == null)
            return;

        List<AnalysisResult> details = analysisDAO.findByStudentId(ev.getStudentId());

        // Build HTML content
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:Segoe UI;font-size:13px;margin:10px;'>");
        sb.append("<h2 style='color:#1A237E;margin-bottom:4px;'>").append(username).append("</h2>");
        sb.append("<table style='margin-bottom:12px;'>");
        sb.append(infoRow("Grade", ev.overallLabel()));
        sb.append(infoRow("DSA Score", String.format("%.2f / 10", ev.getDsaScore())));
        sb.append(infoRow("AI Dependency", String.format("%.2f / 10", ev.getAiDependencyScore())));
        sb.append(infoRow("Analyzed", ev.getTotalAnalyzed() + " submissions"));
        sb.append(infoRow("Top Algorithms", ev.getTopAlgorithms()));
        sb.append(infoRow("Top DS", ev.getTopDataStructures()));
        sb.append("</table>");

        sb.append("<h3 style='color:#1A237E;'>Submission Details (").append(details.size()).append(")</h3>");
        sb.append("<table border='1' cellpadding='5' cellspacing='0' "
                + "style='border-collapse:collapse;width:100%;'>");
        sb.append("<tr style='background:#1A237E;color:white;'>"
                + "<th>Problem</th><th>AI Score</th><th>Complexity</th>"
                + "<th>Algorithms</th><th>Data Structures</th><th>Summary</th></tr>");

        for (int i = 0; i < details.size(); i++) {
            AnalysisResult r = details.get(i);
            String rowBg = i % 2 == 0 ? "#FFFFFF" : "#F5F7FA";
            int ai = r.getAiUsageScore();
            String aiColor = ai >= 7 ? "#C62828" : ai >= 4 ? "#F9A825" : "#00897B";
            sb.append(String.format(
                    "<tr style='background:%s;'><td><b>%s</b></td>"
                            + "<td align='center' style='color:%s;font-weight:bold;'>%d/10</td>"
                            + "<td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",
                    rowBg, esc(r.getProblemName()),
                    aiColor, ai,
                    esc(r.getComplexityEstimate()),
                    esc(r.getAlgorithms()),
                    esc(r.getDataStructures()),
                    esc(r.getSummary())));
        }
        sb.append("</table></body></html>");

        JEditorPane pane = new JEditorPane("text/html", sb.toString());
        pane.setEditable(false);
        pane.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(pane);
        sp.setPreferredSize(new Dimension(1000, 550));
        JOptionPane.showMessageDialog(this, sp, "Detail: " + username, JOptionPane.PLAIN_MESSAGE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String infoRow(String key, String val) {
        return "<tr><td style='color:#546E7A;padding-right:16px;'><b>" + key + "</b></td>"
                + "<td>" + esc(val) + "</td></tr>";
    }

    private String esc(String s) {
        if (s == null)
            return "";
        return s.replace("<", "&lt;").replace(">", "&gt;");
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

    // ── Renderers ─────────────────────────────────────────────────────────────

    private static class ScoreRenderer extends DefaultTableCellRenderer {
        private final boolean higherIsBetter;

        ScoreRenderer(boolean higherIsBetter) {
            this.higherIsBetter = higherIsBetter;
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, f, r, c);
            if (!sel) {
                double val = 0;
                try {
                    val = Double.parseDouble(String.valueOf(v));
                } catch (Exception ignored) {
                }
                Color bg, fg;
                if (higherIsBetter) {
                    bg = val >= 6 ? UIConstants.SUCCESS_LIGHT
                            : val >= 3 ? UIConstants.WARNING_LIGHT : UIConstants.DANGER_LIGHT;
                    fg = val >= 6 ? UIConstants.SUCCESS : val >= 3 ? UIConstants.WARNING : UIConstants.DANGER;
                } else {
                    bg = val <= 3 ? UIConstants.SUCCESS_LIGHT
                            : val <= 6 ? UIConstants.WARNING_LIGHT : UIConstants.DANGER_LIGHT;
                    fg = val <= 3 ? UIConstants.SUCCESS : val <= 6 ? UIConstants.WARNING : UIConstants.DANGER;
                }
                setBackground(bg);
                setForeground(fg);
                setFont(UIConstants.SMALL_BOLD);
            }
            return this;
        }
    }

    private static class GradeRenderer extends DefaultTableCellRenderer {
        GradeRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, f, r, c);
            if (!sel) {
                String s = String.valueOf(v);
                Color bg = switch (s) {
                    case "Tốt" -> UIConstants.SUCCESS_LIGHT;
                    case "Trung bình" -> UIConstants.WARNING_LIGHT;
                    case "Yếu / Nghi ngờ" -> UIConstants.DANGER_LIGHT;
                    default -> UIConstants.CARD_BG;
                };
                Color fg = switch (s) {
                    case "Tốt" -> UIConstants.SUCCESS;
                    case "Trung bình" -> UIConstants.WARNING;
                    case "Yếu / Nghi ngờ" -> UIConstants.DANGER;
                    default -> UIConstants.TEXT;
                };
                setBackground(bg);
                setForeground(fg);
                setFont(UIConstants.SMALL_BOLD);
            }
            return this;
        }
    }
}
