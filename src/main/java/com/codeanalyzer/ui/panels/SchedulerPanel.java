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
 * Panel for controlling the automated Crawl -> AI Analysis -> Evaluate scheduler.
 */
public class SchedulerPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");

    private final CrawlScheduler scheduler = CrawlScheduler.getInstance();

    private final JSpinner spinInterval = new JSpinner(
            new SpinnerNumberModel(AppConfig.getInstance().crawlerScheduleHours(), 1, 24 * 7, 1));

    private final JLabel lblStatus  = new JLabel();
    private final JLabel lblLastRun = new JLabel();
    private final JLabel lblNextRun = new JLabel();
    private final JTextArea logArea = new JTextArea();

    private final StyledButton btnStart = new StyledButton("Start Scheduler", StyledButton.Variant.SUCCESS);
    private final StyledButton btnStop  = new StyledButton("Stop Scheduler",  StyledButton.Variant.DANGER);
    private final StyledButton btnRun   = new StyledButton("Run Now",         StyledButton.Variant.ACCENT);
    private final StyledButton btnClear = new StyledButton("Clear Log",       StyledButton.Variant.NEUTRAL);

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
        JPanel card = new JPanel(new BorderLayout(16, 12));
        card.setBackground(UIConstants.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                new EmptyBorder(18, 20, 18, 20)));

        // Title row
        JLabel title = new JLabel("Automatic Scheduler");
        title.setFont(UIConstants.SUBHEADER);
        title.setForeground(UIConstants.PRIMARY_DARK);

        JLabel desc = new JLabel("<html><span style='color:#546E7A'>"
                + "When enabled, the system automatically runs <b>Crawl &rarr; AI Analysis &rarr; Evaluate</b> "
                + "for all active accounts on the configured interval.<br>"
                + "The crawler uses Selenium (semi-auto), so handle any Cloudflare challenge in the browser window.</span></html>");
        desc.setFont(UIConstants.SMALL);

        JPanel titleBlock = new JPanel(new BorderLayout(0, 6));
        titleBlock.setOpaque(false);
        titleBlock.add(title, BorderLayout.NORTH);
        titleBlock.add(desc,  BorderLayout.CENTER);

        // Controls row
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controls.setOpaque(false);
        controls.setBorder(new EmptyBorder(10, 0, 0, 0));

        JLabel lbl = new JLabel("Interval (hours):");
        lbl.setFont(UIConstants.SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);

        ((JSpinner.DefaultEditor) spinInterval.getEditor()).getTextField().setColumns(4);
        spinInterval.setFont(UIConstants.MAIN);

        controls.add(lbl);
        controls.add(spinInterval);
        controls.add(Box.createHorizontalStrut(16));
        controls.add(btnStart);
        controls.add(btnStop);
        controls.add(btnRun);

        // Status row
        JPanel statusGrid = new JPanel(new GridLayout(3, 1, 0, 4));
        statusGrid.setOpaque(false);
        statusGrid.setBorder(new EmptyBorder(12, 0, 0, 0));

        for (JLabel l : new JLabel[]{lblStatus, lblLastRun, lblNextRun}) {
            l.setFont(UIConstants.MAIN);
            l.setForeground(UIConstants.TEXT_SECONDARY);
            statusGrid.add(l);
        }

        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setOpaque(false);
        content.add(titleBlock,  BorderLayout.NORTH);
        content.add(controls,    BorderLayout.CENTER);
        content.add(statusGrid,  BorderLayout.SOUTH);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ── Log card ──────────────────────────────────────────────────────────────

    private JPanel buildLogCard() {
        logArea.setEditable(false);
        logArea.setFont(UIConstants.MONO);
        logArea.setBackground(UIConstants.CONSOLE_BG);
        logArea.setForeground(UIConstants.CONSOLE_FG);
        logArea.setCaretColor(UIConstants.ACCENT_LIGHT);
        logArea.setBorder(new EmptyBorder(8, 12, 8, 12));

        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createEmptyBorder());

        JLabel header = new JLabel("  Activity Log");
        header.setFont(UIConstants.SMALL_BOLD);
        header.setForeground(UIConstants.CONSOLE_FG);
        header.setBackground(new Color(0x0D1228));
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(0, 26));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(header, BorderLayout.CENTER);
        headerRow.add(btnClear, BorderLayout.EAST);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(UIConstants.CONSOLE_BG);
        inner.add(headerRow, BorderLayout.NORTH);
        inner.add(sp,         BorderLayout.CENTER);
        inner.setBorder(BorderFactory.createLineBorder(new Color(0x1A2A4A)));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(inner, BorderLayout.CENTER);
        return wrap;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void wireActions() {
        btnStart.addActionListener(e -> {
            int h = (int) spinInterval.getValue();
            scheduler.start(h);
            refreshStatus();
        });
        btnStop.addActionListener(e -> { scheduler.stop(); refreshStatus(); });
        btnRun.addActionListener(e -> {
            appendLog("[MANUAL] Triggering one-shot run...");
            scheduler.runNow();
        });
        btnClear.addActionListener(e -> logArea.setText(""));
    }

    private void refreshStatus() {
        boolean on = scheduler.isRunning();
        boolean active = scheduler.isJobActive();

        String statusText = on
                ? "<html><b style='color:#00897B'>RUNNING</b>  (every " + scheduler.getIntervalHours() + " h)"
                  + (active ? "  &mdash;  <i>job in progress...</i>" : "")
                  + "</html>"
                : "<html><b style='color:#C62828'>STOPPED</b></html>";
        lblStatus.setText("Status: " + statusText);

        lblLastRun.setText("Last run:  "
                + (scheduler.getLastRunAt() == null ? "—" : scheduler.getLastRunAt().format(FMT)));
        lblNextRun.setText("Next run:  "
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
