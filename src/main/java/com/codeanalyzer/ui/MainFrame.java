package com.codeanalyzer.ui;

import com.codeanalyzer.ui.components.StatusBar;
import com.codeanalyzer.ui.panels.*;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Code Analyzer System");
        setSize(1180, 760);
        setMinimumSize(new Dimension(1000, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setBackground(UIConstants.BACKGROUND);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);
        add(new StatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                // Gradient indigo → deep indigo
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, UIConstants.PRIMARY,
                        getWidth(), 0, UIConstants.PRIMARY_DARK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 64));
        header.setOpaque(false);

        JLabel title = new JLabel("  🧠  CODE ANALYZER SYSTEM");
        title.setForeground(Color.WHITE);
        title.setFont(UIConstants.HEADER);

        JLabel subtitle = new JLabel("Crawl • Gemini AI Analysis • Đánh giá định kỳ   ");
        subtitle.setForeground(new Color(255, 255, 255, 200));
        subtitle.setFont(UIConstants.SMALL);
        subtitle.setHorizontalAlignment(SwingConstants.RIGHT);

        header.add(title,    BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIConstants.SUBHEADER);
        tabs.setBackground(UIConstants.BACKGROUND);

        tabs.addTab("👤  Quản lý Nick",  new StudentManagementPanel());
        tabs.addTab("📄  Bài Nộp",        new SubmissionPanel());
        tabs.addTab("🧠  Phân Tích AI",   new AnalysisPanel());
        tabs.addTab("📊  Đánh Giá",       new EvaluationPanel());
        tabs.addTab("⏰  Lịch Tự Động",   new SchedulerPanel());

        return tabs;
    }
}
