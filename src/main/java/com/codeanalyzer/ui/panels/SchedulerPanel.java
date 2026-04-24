package com.codeanalyzer.ui.panels;

import com.codeanalyzer.scheduler.CrawlScheduler;
import com.codeanalyzer.ui.UIConstants;
import com.codeanalyzer.ui.components.StyledButton;
import com.codeanalyzer.util.AppConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Panel điều khiển lịch crawl + phân tích + đánh giá tự động.
 *
 *  - Spinner chọn chu kỳ (giờ) [1..168]
 *  - Nút Bật / Tắt / Chạy ngay
 *  - Log realtime qua listener của CrawlScheduler
 *  - Hiển thị trạng thái: đang bật?, lần cuối, lần kế tiếp
 */
public class SchedulerPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private final CrawlScheduler scheduler = CrawlScheduler.getInstance();

    private final JSpinner spinInterval = new JSpinner(
            new SpinnerNumberModel(AppConfig.getInstance().crawlerScheduleHours(), 1, 24 * 7, 1));
    private final JLabel lblStatus   = new JLabel();
    private final JLabel lblLastRun  = new JLabel();
    private final JLabel lblNextRun  = new JLabel();
    private final JTextArea logArea  = new JTextArea();

    private final StyledButton btnStart = new StyledButton("▶  Bật lịch",   StyledButton.Variant.SUCCESS);
    private final StyledButton btnStop  = new StyledButton("⏹  Tắt lịch",   StyledButton.Variant.DANGER);
    private final StyledButton btnRun   = new StyledButton("⚡  Chạy ngay", StyledButton.Variant.ACCENT);
    private final StyledButton btnClear = new StyledButton("🗑  Xoá log",   StyledButton.Variant.NEUTRAL);

    public SchedulerPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(UIConstants.BIG_PADDING, UIConstants.BIG_PADDING,
                                  UIConstants.BIG_PADDING, UIConstants.BIG_PADDING));
        setBackground(UIConstants.BACKGROUND);

        add(buildTopCard(),    BorderLayout.NORTH);
        add(buildLogCard(),    BorderLayout.CENTER);

        scheduler.setListener(this::appendLog);

        wireActions();
        refreshStatus();
        new Timer(2000, e -> refreshStatus()).start();
    }

    // ── UI builders ───────────────────────────────────────────────────────

    private JPanel buildTopCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(10, 10));

        // ── Dòng điều khiển ──────────────────────────────────────────────
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        controls.setOpaque(false);

        JLabel lbl = new JLabel("Chu kỳ (giờ):");
        lbl.setFont(UIConstants.SUBHEADER);
        controls.add(lbl);

        ((JSpinner.DefaultEditor) spinInterval.getEditor())
                .getTextField().setColumns(4);
        spinInterval.setFont(UIConstants.MAIN);
        controls.add(spinInterval);

        controls.add(Box.createHorizontalStrut(16));
        controls.add(btnStart);
        controls.add(btnStop);
        controls.add(btnRun);

        // ── Thông tin trạng thái ────────────────────────────────────────
        JPanel statusPane = new JPanel(new GridLayout(3, 1, 0, 4));
        statusPane.setOpaque(false);
        statusPane.setBorder(new EmptyBorder(4, 4, 0, 4));

        for (JLabel l : new JLabel[]{lblStatus, lblLastRun, lblNextRun}) {
            l.setFont(UIConstants.MAIN);
            l.setForeground(UIConstants.TEXT);
        }
        statusPane.add(lblStatus);
        statusPane.add(lblLastRun);
        statusPane.add(lblNextRun);

        JLabel header = new JLabel("⏰  Lập Lịch Tự Động");
        header.setFont(UIConstants.HEADER.deriveFont(18f));
        header.setForeground(UIConstants.PRIMARY_DARK);
        header.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(header,   BorderLayout.NORTH);
        top.add(controls, BorderLayout.CENTER);
        top.add(statusPane, BorderLayout.SOUTH);

        card.add(top, BorderLayout.CENTER);

        // Mô tả ngắn
        JLabel desc = new JLabel(
                "<html><span style='color:#555'>Khi bật, hệ thống sẽ tự động thực hiện chu trình "
                + "<b>Crawl → Phân tích AI → Đánh giá</b> cho tất cả nick đang hoạt động. "
                + "Crawl dùng Selenium semi-auto nên cần bạn xử lý Cloudflare nếu có.</span></html>");
        desc.setFont(UIConstants.SMALL);
        desc.setBorder(new EmptyBorder(6, 0, 0, 0));
        card.add(desc, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildLogCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(6, 6));

        JLabel header = new JLabel("📜  Log hoạt động");
        header.setFont(UIConstants.SUBHEADER);
        header.setForeground(UIConstants.PRIMARY_DARK);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(header, BorderLayout.WEST);
        headerRow.add(btnClear, BorderLayout.EAST);

        logArea.setEditable(false);
        logArea.setFont(UIConstants.MONO);
        logArea.setBackground(UIConstants.CONSOLE_BG);
        logArea.setForeground(UIConstants.CONSOLE_FG);
        logArea.setCaretColor(Color.WHITE);

        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));

        card.add(headerRow, BorderLayout.NORTH);
        card.add(sp,        BorderLayout.CENTER);
        return card;
    }

    private JPanel card() {
        JPanel c = new JPanel();
        c.setBackground(UIConstants.CARD_BG);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                new EmptyBorder(14, 16, 14, 16)));
        return c;
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void wireActions() {
        btnStart.addActionListener(e -> {
            int h = (int) spinInterval.getValue();
            scheduler.start(h);
            refreshStatus();
        });
        btnStop.addActionListener(e -> { scheduler.stop(); refreshStatus(); });
        btnRun.addActionListener(e -> {
            appendLog("▶ Yêu cầu chạy ngay 1 chu trình...");
            scheduler.runNow();
        });
        btnClear.addActionListener(e -> logArea.setText(""));
    }

    private void refreshStatus() {
        boolean on = scheduler.isRunning();
        lblStatus.setText("Trạng thái: "
                + (on ? "🟢 ĐANG BẬT  (chu kỳ " + scheduler.getIntervalHours() + "h)" : "🔴 TẮT")
                + (scheduler.isJobActive() ? "  –  đang chạy chu trình..." : ""));

        lblLastRun.setText("Lần chạy gần nhất: "
                + (scheduler.getLastRunAt() == null ? "—" : scheduler.getLastRunAt().format(FMT)));
        lblNextRun.setText("Lần chạy kế tiếp:  "
                + (!on || scheduler.getNextRunAt() == null ? "—" : scheduler.getNextRunAt().format(FMT)));

        btnStart.setEnabled(!on);
        btnStop.setEnabled(on);
        spinInterval.setEnabled(!on);
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
