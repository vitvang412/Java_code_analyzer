package com.codeanalyzer.ui.panels;

import com.codeanalyzer.scheduler.CrawlScheduler;
import com.codeanalyzer.ui.UIConstants;
import com.codeanalyzer.ui.components.StyledButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Panel điều khiển lịch tự động: Crawl → AI Analysis → Evaluate.
 * Interval được nhập dạng "X giờ Y phút" trực quan.
 */
public class SchedulerPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");

    private final CrawlScheduler scheduler = CrawlScheduler.getInstance();

    // 2 spinner tách biệt: giờ (0–167) và phút (0–59), tối thiểu 1 phút tổng
    private final JSpinner spinHours   = new JSpinner(new SpinnerNumberModel(
            scheduler.getIntervalMinutes() / 60, 0, 167, 1));
    private final JSpinner spinMinutes = new JSpinner(new SpinnerNumberModel(
            scheduler.getIntervalMinutes() % 60, 0, 59, 5));

    // Status labels
    private final JLabel lblStatusDot  = new JLabel("●");
    private final JLabel lblStatusText = new JLabel("STOPPED");
    private final JLabel lblInterval   = new JLabel("—");
    private final JLabel lblLastRun    = new JLabel("—");
    private final JLabel lblNextRun    = new JLabel("—");
    private final JTextArea logArea    = new JTextArea();

    private final StyledButton btnStart = new StyledButton("Start Scheduler", StyledButton.Variant.SUCCESS);
    private final StyledButton btnStop  = new StyledButton("Stop Scheduler",  StyledButton.Variant.DANGER);
    private final StyledButton btnRun   = new StyledButton("Run Now",          StyledButton.Variant.ACCENT);
    private final StyledButton btnClear = new StyledButton("Clear",              StyledButton.Variant.NEUTRAL);

    public SchedulerPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UIConstants.BACKGROUND);
        setBorder(new EmptyBorder(16, 18, 16, 18));

        add(buildControlCard(), BorderLayout.NORTH);
        add(buildLogCard(),     BorderLayout.CENTER);

        scheduler.setListener(this::appendLog);
        wireActions();
        refreshStatus();
        new Timer(2000, e -> refreshStatus()).start();
    }

    // ── Control card ──────────────────────────────────────────────────────────

    private JPanel buildControlCard() {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(UIConstants.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                new EmptyBorder(20, 24, 20, 24)));

        // ── Title ──
        JLabel title = new JLabel("⏰  Automatic Scheduler");
        title.setFont(UIConstants.SUBHEADER);
        title.setForeground(UIConstants.PRIMARY_DARK);

        JLabel desc = new JLabel("<html><span style='color:#607D8B'>"
                + "Tự động chạy <b>Crawl → AI Analysis → Evaluate</b> định kỳ cho tất cả tài khoản. "
                + "Crawler dùng Selenium – hãy xử lý Cloudflare nếu được hỏi.</span></html>");
        desc.setFont(UIConstants.SMALL);

        JPanel titleBlock = new JPanel(new BorderLayout(0, 6));
        titleBlock.setOpaque(false);
        titleBlock.add(title, BorderLayout.NORTH);
        titleBlock.add(desc,  BorderLayout.CENTER);

        // ── Interval row: "Lặp lại mỗi: [0] giờ [30] phút" ──
        JPanel intervalRow = buildIntervalRow();

        // ── Button row ──
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnStart);
        btnRow.add(btnStop);
        btnRow.add(btnRun);

        // ── Status badges ──
        JPanel statusRow = buildStatusRow();

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(intervalRow, BorderLayout.NORTH);
        content.add(btnRow,      BorderLayout.CENTER);
        content.add(statusRow,   BorderLayout.SOUTH);

        card.add(titleBlock, BorderLayout.NORTH);
        card.add(content,    BorderLayout.CENTER);
        return card;
    }

    /** Dòng nhập interval dạng "Lặp lại mỗi: [ 1 ] giờ  [ 30 ] phút" */
    private JPanel buildIntervalRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 0, 4, 0));

        JLabel lbl = new JLabel("Lặp lại mỗi:");
        lbl.setFont(UIConstants.SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);

        styleSpinner(spinHours);
        styleSpinner(spinMinutes);

        JLabel lblH = new JLabel("giờ");
        JLabel lblM = new JLabel("phút");
        lblH.setFont(UIConstants.MAIN);
        lblH.setForeground(UIConstants.TEXT_SECONDARY);
        lblM.setFont(UIConstants.MAIN);
        lblM.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel hint = new JLabel("   (tối thiểu 1 phút; 0 giờ 0 phút không hợp lệ)");
        hint.setFont(UIConstants.LABEL);
        hint.setForeground(UIConstants.TEXT_MUTED);

        row.add(lbl);
        row.add(spinHours);
        row.add(lblH);
        row.add(Box.createHorizontalStrut(6));
        row.add(spinMinutes);
        row.add(lblM);
        row.add(hint);
        return row;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(UIConstants.MAIN);
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(3);
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);
    }

    /** Dải trạng thái dạng badge card ngang. */
    private JPanel buildStatusRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(4, 0, 0, 0));

        row.add(buildBadgeCard("Trạng thái",    buildStatusBadge()));
        row.add(Box.createHorizontalStrut(10));
        row.add(buildBadgeCard("Chu kỳ",        lblInterval));
        row.add(Box.createHorizontalStrut(10));
        row.add(buildBadgeCard("Lần cuối chạy", lblLastRun));
        row.add(Box.createHorizontalStrut(10));
        row.add(buildBadgeCard("Lần kế tiếp",   lblNextRun));
        return row;
    }

    private JPanel buildBadgeCard(String caption, JComponent valueWidget) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setBackground(UIConstants.BACKGROUND);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                new EmptyBorder(8, 14, 8, 14)));
        JLabel cap = new JLabel(caption);
        cap.setFont(UIConstants.LABEL);
        cap.setForeground(UIConstants.TEXT_MUTED);
        p.add(cap,         BorderLayout.NORTH);
        p.add(valueWidget, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildStatusBadge() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        lblStatusDot.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lblStatusText.setFont(UIConstants.SMALL_BOLD);
        p.add(lblStatusDot);
        p.add(lblStatusText);
        return p;
    }

    // ── Log card ──────────────────────────────────────────────────────────────

    private JPanel buildLogCard() {
        logArea.setEditable(false);
        logArea.setFont(UIConstants.MONO);
        logArea.setBackground(UIConstants.CONSOLE_BG);
        logArea.setForeground(UIConstants.CONSOLE_FG);
        logArea.setCaretColor(UIConstants.ACCENT_LIGHT);
        logArea.setBorder(new EmptyBorder(10, 14, 10, 14));

        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(UIConstants.CONSOLE_BG);

        JLabel header = new JLabel("  📋  Activity Log");
        header.setFont(UIConstants.SMALL_BOLD);
        header.setForeground(new Color(0x8B949E));
        header.setBackground(new Color(0x161B22));
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(0, 30));
        header.setBorder(new EmptyBorder(0, 8, 0, 0));

        JPanel headerRow = new JPanel(new BorderLayout(8, 0));
        headerRow.setOpaque(false);
        headerRow.add(header,   BorderLayout.CENTER);
        headerRow.add(btnClear, BorderLayout.EAST);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(UIConstants.CONSOLE_BG);
        inner.add(headerRow, BorderLayout.NORTH);
        inner.add(sp,        BorderLayout.CENTER);
        inner.setBorder(BorderFactory.createLineBorder(new Color(0x30363D)));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(inner, BorderLayout.CENTER);
        return wrap;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void wireActions() {
        btnStart.addActionListener(e -> {
            int totalMins = getTotalMinutes();
            if (totalMins <= 0) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng đặt ít nhất 1 phút cho chu kỳ lặp.",
                        "Interval không hợp lệ", JOptionPane.WARNING_MESSAGE);
                return;
            }
            scheduler.start(totalMins);
            refreshStatus();
        });
        btnStop.addActionListener(e -> { scheduler.stop(); refreshStatus(); });
        btnRun.addActionListener(e -> {
            appendLog("[MANUAL] Kích hoạt chạy ngay...");
            scheduler.runNow();
        });
        btnClear.addActionListener(e -> logArea.setText(""));
    }

    /** Lấy tổng phút từ 2 spinner. */
    private int getTotalMinutes() {
        int h = (int) spinHours.getValue();
        int m = (int) spinMinutes.getValue();
        return h * 60 + m;
    }

    private void refreshStatus() {
        SwingUtilities.invokeLater(() -> {
            boolean on     = scheduler.isRunning();
            boolean active = scheduler.isJobActive();

            if (on) {
                lblStatusDot.setForeground(new Color(0x059669));
                lblStatusText.setForeground(new Color(0x059669));
                lblStatusText.setText(active ? "ĐANG CHẠY…" : "RUNNING");
            } else {
                lblStatusDot.setForeground(new Color(0xC62828));
                lblStatusText.setForeground(new Color(0xC62828));
                lblStatusText.setText("STOPPED");
            }

            int mins = scheduler.getIntervalMinutes();
            lblInterval.setText(on ? CrawlScheduler.formatInterval(mins) : "—");
            lblInterval.setFont(UIConstants.SMALL_BOLD);
            lblInterval.setForeground(UIConstants.TEXT);

            lblLastRun.setFont(UIConstants.SMALL_BOLD);
            lblLastRun.setForeground(UIConstants.TEXT);
            lblNextRun.setFont(UIConstants.SMALL_BOLD);
            lblNextRun.setForeground(on ? UIConstants.ACCENT : UIConstants.TEXT_MUTED);

            lblLastRun.setText(scheduler.getLastRunAt() == null
                    ? "—" : scheduler.getLastRunAt().format(FMT));
            lblNextRun.setText(!on || scheduler.getNextRunAt() == null
                    ? "—" : scheduler.getNextRunAt().format(FMT));

            btnStart.setEnabled(!on);
            btnStop.setEnabled(on);
            spinHours.setEnabled(!on);
            spinMinutes.setEnabled(!on);
        });
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
