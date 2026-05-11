package com.codeanalyzer.scheduler;

import com.codeanalyzer.ai.CodeAnalyzer;
import com.codeanalyzer.ai.StudentEvaluator;
import com.codeanalyzer.crawler.CrawlerService;
import com.codeanalyzer.database.StudentDAO;
import com.codeanalyzer.database.SubmissionDAO;
import com.codeanalyzer.model.Student;
import com.codeanalyzer.model.Submission;
import com.codeanalyzer.util.AppConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler chạy định kỳ 1 chu trình "Crawl → Analyze → Evaluate".
 * Hỗ trợ interval theo PHÚT (tối thiểu 1 phút).
 *
 * Singleton: chỉ có 1 scheduler trong toàn app.
 */
public class CrawlScheduler {

    /** Listener để UI nhận thông báo trạng thái. */
    public interface Listener {
        void onEvent(String message);
    }

    private static final CrawlScheduler INSTANCE = new CrawlScheduler();
    public static CrawlScheduler getInstance() { return INSTANCE; }

    private final StudentDAO       studentDAO       = new StudentDAO();
    private final SubmissionDAO    submissionDAO    = new SubmissionDAO();
    private final CrawlerService   crawlerService   = new CrawlerService();
    private final CodeAnalyzer     codeAnalyzer     = new CodeAnalyzer();
    private final StudentEvaluator studentEvaluator = new StudentEvaluator();

    private ScheduledExecutorService executor;
    private ScheduledFuture<?>       scheduledTask;
    private volatile int             intervalMinutes;   // interval theo PHÚT
    private volatile LocalDateTime   lastRunAt;
    private volatile LocalDateTime   nextRunAt;
    private volatile boolean         running     = false;
    private volatile boolean         jobInProgress = false;
    private volatile Listener        listener;

    private CrawlScheduler() {
        // Default: đọc từ config (đơn vị giờ → chuyển sang phút)
        this.intervalMinutes = AppConfig.getInstance().crawlerScheduleHours() * 60;
    }

    // ────────────────────────────────────────────────────────────────────
    // Public control API
    // ────────────────────────────────────────────────────────────────────

    public synchronized void setListener(Listener l)       { this.listener = l; }
    public synchronized boolean isRunning()                { return running; }
    public synchronized boolean isJobActive()              { return jobInProgress; }
    public synchronized int     getIntervalMinutes()       { return intervalMinutes; }
    public synchronized LocalDateTime getLastRunAt()       { return lastRunAt; }
    public synchronized LocalDateTime getNextRunAt()       { return nextRunAt; }

    /**
     * Bắt đầu lịch (hoặc restart với chu kỳ mới).
     * @param intervalMinutes chu kỳ tính bằng PHÚT (tối thiểu 1)
     */
    public synchronized void start(int intervalMinutes) {
        if (intervalMinutes <= 0) {
            log("Chu kỳ không hợp lệ (" + intervalMinutes + " phút). Huỷ.");
            return;
        }
        this.intervalMinutes = intervalMinutes;
        stopInternal();

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "crawl-scheduler");
            t.setDaemon(true);
            return t;
        });

        long periodSec = (long) intervalMinutes * 60L;
        // initialDelay = period để tránh chạy ngay lập tức khi bật
        scheduledTask = executor.scheduleAtFixedRate(
                this::runJob, periodSec, periodSec, TimeUnit.SECONDS);

        running   = true;
        nextRunAt = LocalDateTime.now().plusMinutes(intervalMinutes);
        log("[START] Lịch tự động đã bật (mỗi " + formatInterval(intervalMinutes)
                + "). Lần chạy kế: " + nextRunAt);
    }

    public synchronized void stop() {
        stopInternal();
        log("[STOP] Đã tắt lịch tự động.");
    }

    private void stopInternal() {
        if (scheduledTask != null) { scheduledTask.cancel(false); scheduledTask = null; }
        if (executor != null)      { executor.shutdown(); executor = null; }
        running   = false;
        nextRunAt = null;
    }

    /** Chạy ngay 1 lần trên background thread. */
    public synchronized void runNow() {
        if (jobInProgress) {
            log("Một lần chạy đang diễn ra, bỏ qua yêu cầu 'Chạy ngay'.");
            return;
        }
        Thread t = new Thread(this::runJob, "crawl-scheduler-now");
        t.setDaemon(true);
        t.start();
    }

    // ────────────────────────────────────────────────────────────────────
    // Core job
    // ────────────────────────────────────────────────────────────────────

    private void runJob() {
        synchronized (this) {
            if (jobInProgress) return;
            jobInProgress = true;
        }
        try {
            lastRunAt = LocalDateTime.now();
            log("══════════════════════════════════");
            log("[" + lastRunAt + "] Bắt đầu chu trình crawl + phân tích.");

            List<Student> students = studentDAO.findAll();
            if (students.isEmpty()) {
                log("Không có sinh viên nào – bỏ qua."); return;
            }

            for (Student s : students) {
                log("────────────────────────────────────────");
                log("Sinh viên: " + s.getUsername());

                try {
                    log("  (1) Đang cào bài nộp...");
                    crawlerService.startCrawl(s);
                } catch (Exception ex) {
                    log("  Lỗi khi cào: " + ex.getMessage()); continue;
                }

                try {
                    List<Submission> pending = submissionDAO.findUnanalyzedByStudent(s.getId(), 200);
                    log("  (2) Gemini phân tích " + pending.size() + " bài chưa phân tích.");
                    for (Submission sub : pending) {
                        codeAnalyzer.analyzeSubmission(sub);
                        try { Thread.sleep(4500); } catch (InterruptedException ignored) {}
                    }
                } catch (Exception ex) {
                    log("  Lỗi khi phân tích: " + ex.getMessage());
                }

                try {
                    log("  (3) Tổng hợp đánh giá...");
                    studentEvaluator.evaluateStudent(s);
                } catch (Exception ex) {
                    log("  Lỗi khi đánh giá: " + ex.getMessage());
                }
            }

            log("✅ Hoàn tất chu trình lúc " + LocalDateTime.now());
            if (running) {
                nextRunAt = LocalDateTime.now().plusMinutes(intervalMinutes);
                log("Lần chạy kế: " + nextRunAt);
            }
        } catch (Throwable t) {
            log("Lỗi không mong đợi: " + t.getMessage());
            t.printStackTrace();
        } finally {
            synchronized (this) { jobInProgress = false; }
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────

    /** Hiển thị "2 giờ 30 phút" hoặc "45 phút" tùy giá trị. */
    public static String formatInterval(int minutes) {
        if (minutes < 60) return minutes + " phút";
        int h = minutes / 60, m = minutes % 60;
        return m == 0 ? h + " giờ" : h + " giờ " + m + " phút";
    }

    private void log(String msg) {
        System.out.println("[Scheduler] " + msg);
        Listener l = this.listener;
        if (l != null) { try { l.onEvent(msg); } catch (Exception ignored) {} }
    }
}
