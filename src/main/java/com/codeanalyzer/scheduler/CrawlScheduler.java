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
 *
 * Singleton: chỉ có 1 scheduler trong toàn app để UI (SchedulerPanel)
 * và main app chia sẻ cùng trạng thái.
 *
 * Dùng ScheduledExecutorService (single-threaded) để các lần chạy không bao giờ
 * chồng lấn nhau – nếu task chạy lâu hơn chu kỳ, lần kế tiếp sẽ đợi.
 */
public class CrawlScheduler {

    /** Listener để UI nhận thông báo trạng thái. */
    public interface Listener {
        void onEvent(String message);
    }

    private static final CrawlScheduler INSTANCE = new CrawlScheduler();
    public static CrawlScheduler getInstance() { return INSTANCE; }

    private final StudentDAO        studentDAO        = new StudentDAO();
    private final SubmissionDAO     submissionDAO     = new SubmissionDAO();
    private final CrawlerService    crawlerService    = new CrawlerService();
    private final CodeAnalyzer      codeAnalyzer      = new CodeAnalyzer();
    private final StudentEvaluator  studentEvaluator  = new StudentEvaluator();

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledTask;
    private volatile int intervalHours;
    private volatile LocalDateTime lastRunAt;
    private volatile LocalDateTime nextRunAt;
    private volatile boolean running = false;
    private volatile boolean jobInProgress = false;
    private volatile Listener listener;

    private CrawlScheduler() {
        this.intervalHours = AppConfig.getInstance().crawlerScheduleHours();
    }

    // ────────────────────────────────────────────────────────────────────
    // Public control API
    // ────────────────────────────────────────────────────────────────────

    public synchronized void setListener(Listener l) {
        this.listener = l;
    }

    public synchronized boolean isRunning()    { return running; }
    public synchronized boolean isJobActive()  { return jobInProgress; }
    public synchronized int getIntervalHours() { return intervalHours; }
    public synchronized LocalDateTime getLastRunAt() { return lastRunAt; }
    public synchronized LocalDateTime getNextRunAt() { return nextRunAt; }

    /**
     * Bắt đầu lịch (hoặc restart với chu kỳ mới).
     * Lần chạy đầu tiên sẽ xảy ra sau {@code intervalHours} giờ, KHÔNG chạy ngay
     * – tránh bất ngờ mở Chrome khi vừa bật app. Nếu muốn chạy ngay: {@link #runNow()}.
     */
    public synchronized void start(int intervalHours) {
        if (intervalHours <= 0) {
            log("Chu kỳ không hợp lệ (" + intervalHours + "). Huỷ.");
            return;
        }
        this.intervalHours = intervalHours;

        stopInternal(); // cancel old task if any

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "crawl-scheduler");
            t.setDaemon(true);
            return t;
        });

        long periodSec = (long) intervalHours * 3600L;
        // initialDelay = period để tránh chạy ngay lập tức
        scheduledTask = executor.scheduleAtFixedRate(
                this::runJob, periodSec, periodSec, TimeUnit.SECONDS);

        running = true;
        nextRunAt = LocalDateTime.now().plusHours(intervalHours);
        log("▶️  Lịch tự động đã bật (mỗi " + intervalHours + "h). Lần chạy kế: " + nextRunAt);
    }

    public synchronized void stop() {
        stopInternal();
        log("⏹️  Đã tắt lịch tự động.");
    }

    private void stopInternal() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        running = false;
        nextRunAt = null;
    }

    /** Chạy ngay 1 lần trên background thread (không ảnh hưởng schedule). */
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
    // Core job: crawl + analyze + evaluate for every active student
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
                log("Không có sinh viên nào trong hệ thống – bỏ qua.");
                return;
            }

            for (Student s : students) {
                log("────────────────────────────────────────");
                log("Sinh viên: " + s.getUsername());

                // ── 1. Crawl mới ──────────────────────────────────────────
                try {
                    log("  (1) Đang cào bài nộp...");
                    crawlerService.startCrawl(s);
                } catch (Exception ex) {
                    log("  Lỗi khi cào: " + ex.getMessage());
                    continue;
                }

                // ── 2. Phân tích những bài chưa có analysis ───────────────
                try {
                    List<Submission> pending =
                            submissionDAO.findUnanalyzedByStudent(s.getId(), 200);
                    log("  (2) Gemini phân tích " + pending.size() + " bài chưa được phân tích.");
                    for (Submission sub : pending) {
                        codeAnalyzer.analyzeSubmission(sub);
                        // Nghỉ giữa các request để tránh rate-limit Gemini
                        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    }
                } catch (Exception ex) {
                    log("  Lỗi khi phân tích: " + ex.getMessage());
                }

                // ── 3. Đánh giá tổng thể sinh viên ────────────────────────
                try {
                    log("  (3) Tổng hợp đánh giá...");
                    studentEvaluator.evaluateStudent(s);
                } catch (Exception ex) {
                    log("  Lỗi khi đánh giá: " + ex.getMessage());
                }
            }

            log("✅ Hoàn tất chu trình lúc " + LocalDateTime.now());
            if (running) {
                nextRunAt = LocalDateTime.now().plusHours(intervalHours);
                log("Lần chạy kế: " + nextRunAt);
            }
        } catch (Throwable t) {
            log("Lỗi không mong đợi: " + t.getMessage());
            t.printStackTrace();
        } finally {
            synchronized (this) { jobInProgress = false; }
        }
    }

    private void log(String msg) {
        System.out.println("[Scheduler] " + msg);
        Listener l = this.listener;
        if (l != null) {
            try { l.onEvent(msg); } catch (Exception ignored) {}
        }
    }
}
