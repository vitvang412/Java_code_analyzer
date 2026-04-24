package com.codeanalyzer.ui.components;

import com.codeanalyzer.scheduler.CrawlScheduler;
import com.codeanalyzer.ui.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Thanh trạng thái ở chân cửa sổ: hiển thị thông điệp chung + trạng thái scheduler.
 * Tự cập nhật mỗi 5 giây.
 */
public class StatusBar extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    private final JLabel leftLabel  = new JLabel("Hệ thống đã sẵn sàng.");
    private final JLabel rightLabel = new JLabel();

    public StatusBar() {
        setLayout(new BorderLayout());
        setBackground(new Color(0xECEFF1));
        setBorder(new EmptyBorder(4, 10, 4, 10));
        setPreferredSize(new Dimension(0, 28));

        leftLabel.setFont(UIConstants.SMALL);
        leftLabel.setForeground(UIConstants.TEXT_MUTED);
        rightLabel.setFont(UIConstants.SMALL);
        rightLabel.setForeground(UIConstants.TEXT_MUTED);
        rightLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        add(leftLabel,  BorderLayout.WEST);
        add(rightLabel, BorderLayout.EAST);

        new Timer(5000, e -> refreshSchedulerInfo()).start();
        refreshSchedulerInfo();
    }

    public void setMessage(String msg) { leftLabel.setText(msg); }

    private void refreshSchedulerInfo() {
        CrawlScheduler s = CrawlScheduler.getInstance();
        StringBuilder sb = new StringBuilder();
        if (s.isRunning()) {
            sb.append("⏱ Lịch: ON (").append(s.getIntervalHours()).append("h)");
            if (s.getNextRunAt() != null) {
                sb.append("  |  Kế: ").append(s.getNextRunAt().format(FMT));
            }
        } else {
            sb.append("⏱ Lịch: OFF");
        }
        if (s.isJobActive()) sb.append("  |  Đang chạy...");
        rightLabel.setText(sb.toString());
    }
}
