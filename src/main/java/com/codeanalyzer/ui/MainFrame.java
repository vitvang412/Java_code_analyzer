package com.codeanalyzer.ui;

import com.codeanalyzer.ui.components.StatusBar;
import com.codeanalyzer.ui.panels.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;


public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Code Analyzer System  —  Codeforces + Gemini AI");
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 680));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UIConstants.BACKGROUND);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        add(new StatusBar(), BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient: deep slate → lighter slate
                GradientPaint gp = new GradientPaint(
                        0, 0, UIConstants.PRIMARY_DARK,
                        getWidth(), 0, UIConstants.PRIMARY);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Bottom accent line (violet)
                g2.setColor(UIConstants.ACCENT);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 70));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        // Left: icon + title
        JPanel leftBlock = new JPanel(new BorderLayout(12, 0));
        leftBlock.setOpaque(false);

        // Circular icon
        JPanel iconCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.ACCENT);
                g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                String text = "CA";
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x, y);
                g2.dispose();
            }
        };
        iconCircle.setPreferredSize(new Dimension(46, 46));
        iconCircle.setOpaque(false);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 1));
        titleBlock.setOpaque(false);

        JLabel titleLbl = new JLabel("CODE ANALYZER SYSTEM");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(Color.WHITE);

        JLabel subLbl = new JLabel("Codeforces Crawler  •  Gemini AI Analysis  •  Student Evaluation");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(255, 255, 255, 180));

        titleBlock.add(titleLbl);
        titleBlock.add(subLbl);

        leftBlock.add(iconCircle, BorderLayout.WEST);
        leftBlock.add(titleBlock, BorderLayout.CENTER);

        // Right: version badge
        JLabel verBadge = new JLabel("v1.0");
        verBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        verBadge.setForeground(UIConstants.ACCENT_LIGHT);
        verBadge.setHorizontalAlignment(SwingConstants.RIGHT);

        header.add(leftBlock, BorderLayout.WEST);
        header.add(verBadge, BorderLayout.EAST);
        return header;
    }

    // ── Tabbed pane ───────────────────────────────────────────────────────────

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(UIConstants.BACKGROUND);
        tabs.setForeground(UIConstants.TEXT);

        // Custom UI for cleaner tabs
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                    int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected) {
                    g2.setColor(UIConstants.CARD_BG);
                } else {
                    g2.setColor(new Color(0xE2E8F0));
                }
                g2.fillRect(x, y, w, h);
                if (isSelected) {
                    g2.setColor(UIConstants.ACCENT);
                    g2.fillRect(x, y + h - 3, w, 3);
                }
                g2.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                    int x, int y, int w, int h, boolean isSelected) {
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                    int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
            }
        });

        tabs.addTab("  Manage Accounts  ", new StudentManagementPanel());
        tabs.addTab("  Submissions  ", new SubmissionPanel());
        tabs.addTab("  AI Analysis  ", new AnalysisPanel());
        tabs.addTab("  Evaluations  ", new EvaluationPanel());
        tabs.addTab("  Scheduler  ", new SchedulerPanel());

        return tabs;
    }
}
