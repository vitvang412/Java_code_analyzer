package com.codeanalyzer.ui.components;

import com.codeanalyzer.scheduler.CrawlScheduler;
import com.codeanalyzer.ui.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Bottom status bar – shows system status and scheduler state.
 * Auto-refreshes every 5 seconds.
 */
public class StatusBar extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    private final JLabel leftLabel  = new JLabel("System ready.");
    private final JLabel rightLabel = new JLabel();
    private final JPanel dot        = new JPanel();

    public StatusBar() {
        setLayout(new BorderLayout(8, 0));
        setBackground(UIConstants.PRIMARY_DARK);
        setBorder(new EmptyBorder(4, 12, 4, 12));
        setPreferredSize(new Dimension(0, 28));

        leftLabel.setFont(UIConstants.SMALL);
        leftLabel.setForeground(new Color(0xB0BEC5));

        rightLabel.setFont(UIConstants.SMALL);
        rightLabel.setForeground(new Color(0xB0BEC5));
        rightLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        // Indicator dot
        dot.setPreferredSize(new Dimension(8, 8));
        dot.setMaximumSize(new Dimension(8, 8));
        dot.setBorder(BorderFactory.createEmptyBorder());
        dot.setOpaque(true);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(dot);
        left.add(leftLabel);

        add(left, BorderLayout.WEST);
        add(rightLabel, BorderLayout.EAST);

        new Timer(5000, e -> refreshSchedulerInfo()).start();
        refreshSchedulerInfo();
    }

    public void setMessage(String msg) {
        SwingUtilities.invokeLater(() -> leftLabel.setText(msg));
    }

    private void refreshSchedulerInfo() {
        CrawlScheduler s = CrawlScheduler.getInstance();
        StringBuilder sb = new StringBuilder();
        if (s.isRunning()) {
            dot.setBackground(new Color(0x00E676));  // green
            sb.append("Scheduler ON  (").append(s.getIntervalHours()).append("h)");
            if (s.getNextRunAt() != null) {
                sb.append("   Next: ").append(s.getNextRunAt().format(FMT));
            }
        } else {
            dot.setBackground(new Color(0xEF9A9A));  // soft red
            sb.append("Scheduler OFF");
        }
        if (s.isJobActive()) sb.append("   |   Running...");
        rightLabel.setText(sb.toString());
    }
}
