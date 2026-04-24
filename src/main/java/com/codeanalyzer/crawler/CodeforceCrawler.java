package com.codeanalyzer.crawler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.codeanalyzer.database.StudentDAO;
import com.codeanalyzer.database.SubmissionDAO;
import com.codeanalyzer.model.Student;
import com.codeanalyzer.model.Submission;
import com.codeanalyzer.util.AppConfig;

/**
 * Semi-automatic Codeforces crawler using Selenium + Chrome.
 * <p>
 * Flow:
 *  1. Open Chrome with the user's real Chrome profile (keeps cookies / login).
 *  2. Navigate to the student's submission list.
 *  3. PAUSE – user solves any Cloudflare / CAPTCHA challenge in the browser.
 *  4. After user signals "ready", crawl ALL pages of accepted submissions.
 *  5. For each accepted submission, open its detail page and save the source code.
 */
public class CodeforceCrawler {

    // ──────────────────────────────────────────────────────────────────────────
    // CSS / XPath selectors (based on Codeforces HTML as of 2024)
    // ──────────────────────────────────────────────────────────────────────────
    private static final String SEL_SUBMISSION_ROW   = "table.status-frame-datatable tr";
    private static final String SEL_VERDICT_ACCEPTED = "span.verdict-accepted";
    private static final String SEL_SOURCE_CODE      = "pre#program-source-text";
    private static final String SEL_NEXT_PAGE        = "span.page-index a";           // next-page link

    private final AppConfig     cfg          = AppConfig.getInstance();
    private final SubmissionDAO submissionDAO = new SubmissionDAO();
    private final StudentDAO    studentDAO   = new StudentDAO();

    // ──────────────────────────────────────────────────────────────────────────
    // Public entry point
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Crawls all accepted submissions for the given student.
     * Blocks until crawling is complete (call from a background thread).
     *
     * @param student   the student to crawl
     * @param statusCallback  optional callback invoked with status messages for the UI (can be null)
     */
    public void crawlForStudent(Student student, CrawlStatusCallback statusCallback) {
        log(statusCallback, "Bắt đầu cào dữ liệu cho: " + student.getUsername());

        WebDriver driver = buildDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            // ── Step 1: Open the submissions page ────────────────────────────
            String submissionsUrl = "https://codeforces.com/submissions/" + student.getUsername();
            driver.get(submissionsUrl);
            log(statusCallback, "Đã mở trang: " + submissionsUrl);

            // ── Step 2: Pause for human interaction ──────────────────────────
            log(statusCallback, "⚠️  Chrome đã mở. Kiểm tra trình duyệt!");
            log(statusCallback, "   Nếu Cloudflare chặn → nhấn 'I am human' rồi quay lại.");
            log(statusCallback, "   Khi thấy bảng bài nộp hiện ra → nhấn OK trong hộp thoại bên dưới.");

            waitForOkDialog(student.getUsername());

            // ── Step 3: Collect submission metadata from ALL pages ────────────
            log(statusCallback, "Đang thu thập danh sách bài nộp...");
            List<SubmissionMeta> metas = collectAllSubmissions(driver, wait, statusCallback);
            log(statusCallback, "Tìm thấy " + metas.size() + " bài Accepted.");

            // ── Step 4: For each submission, open detail page & save ─────────
            int saved = 0, skipped = 0;
            for (int i = 0; i < metas.size(); i++) {
                SubmissionMeta m = metas.get(i);
                log(statusCallback, String.format("[%d/%d] Đang cào: %s (%s)",
                        i + 1, metas.size(), m.problemName, m.submissionId));

                try {
                    String sourceCode = fetchSourceCode(driver, wait, m);
                    if (sourceCode == null || sourceCode.isBlank()) {
                        log(statusCallback, "  → Không lấy được source code, bỏ qua.");
                        skipped++;
                        continue;
                    }

                    Submission sub = new Submission(
                        student.getId(),
                        m.problemId,
                        m.problemName,
                        m.language,
                        sourceCode,
                        "Accepted",
                        LocalDateTime.now()
                    );
                    submissionDAO.save(sub);
                    saved++;
                    log(statusCallback, "  → Đã lưu.");

                    Thread.sleep(cfg.crawlerDelayMs()); // polite delay
                } catch (Exception ex) {
                    log(statusCallback, "  → Lỗi: " + ex.getMessage());
                    skipped++;
                }
            }

            // ── Step 5: Update last-crawled timestamp ─────────────────────────
            studentDAO.updateLastCrawled(student.getId(), LocalDateTime.now());
            log(statusCallback, String.format("✅ Hoàn tất! Đã lưu %d bài, bỏ qua %d bài.", saved, skipped));

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log(statusCallback, "Crawler bị gián đoạn.");
        } catch (Exception e) {
            log(statusCallback, "Lỗi nghiêm trọng: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    // Overload for callers that don't need status callbacks
    public void crawlForStudent(Student student) {
        crawlForStudent(student, null);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 3 helper: walk every page of the submissions list
    // ──────────────────────────────────────────────────────────────────────────

    private List<SubmissionMeta> collectAllSubmissions(
            WebDriver driver, WebDriverWait wait, CrawlStatusCallback cb) throws InterruptedException {

        List<SubmissionMeta> all = new ArrayList<>();
        int page = 1;

        while (true) {
            log(cb, "  Đang đọc trang " + page + " danh sách bài nộp...");

            // Wait until the table is present
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.status-frame-datatable")));
                System.out.println("[DEBUG] ✅ Tìm thấy bảng status-frame-datatable");
            } catch (Exception e) {
                log(cb, "  Không tìm thấy bảng bài nộp trên trang " + page + ". Có thể do mạng chậm hoặc sai tên tài khoản.");
                // DEBUG: check what tables exist
                List<WebElement> allTables = driver.findElements(By.tagName("table"));
                System.out.println("[DEBUG] Tìm thấy " + allTables.size() + " tables: ");
                for (WebElement t : allTables) {
                    System.out.println("  - " + t.getAttribute("class"));
                }
                break;
            }

            List<WebElement> rows = driver.findElements(By.cssSelector(SEL_SUBMISSION_ROW));
            System.out.println("[DEBUG] Tìm thấy " + rows.size() + " rows trong bảng");
            for (WebElement row : rows) {
                SubmissionMeta meta = parseRow(row);
                if (meta != null) all.add(meta);
            }

            // Try to go to the next page
            List<WebElement> nextLinks = driver.findElements(By.cssSelector(SEL_NEXT_PAGE));
            if (nextLinks.isEmpty()) break;

            String nextHref = nextLinks.get(0).getAttribute("href");
            driver.get(nextHref);
            Thread.sleep(1500); // wait for page to load
            page++;
        }

        return all;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Parse a single <tr> row into a SubmissionMeta
    // Returns null if the submission is not Accepted
    // ──────────────────────────────────────────────────────────────────────────

    private SubmissionMeta parseRow(WebElement row) {
        try {
            // Only process accepted submissions
            List<WebElement> acceptedSpans = row.findElements(By.cssSelector(SEL_VERDICT_ACCEPTED));
            
            // DEBUG: log all span classes to see what verdicts are present
            List<WebElement> allSpans = row.findElements(By.tagName("span"));
            if (!allSpans.isEmpty()) {
                for (WebElement span : allSpans) {
                    String className = span.getAttribute("class");
                    System.out.println("[DEBUG] Span class: " + className + " | Text: " + span.getText());
                }
            }
            
            if (!acceptedSpans.isEmpty()) {
                System.out.println("[DEBUG] ✅ Tìm thấy accepted submission!");
            }
            if (acceptedSpans.isEmpty()) {
                System.out.println("[DEBUG] Row này không có verdict-accepted, bỏ qua");
                return null;
            }
            
            // DEBUG: log all links in this row
            List<WebElement> allLinks = row.findElements(By.tagName("a"));
            System.out.println("[DEBUG] Row có " + allLinks.size() + " links:");
            for (WebElement link : allLinks) {
                String href = link.getAttribute("href");
                String text = link.getText();
                System.out.println("[DEBUG]   Link: " + text + " -> " + href);
            }

            String submissionId = row.getAttribute("data-submission-id");
            if (submissionId == null || submissionId.isBlank()) {
                submissionId = row.getAttribute("data-submissionid");
            }
            System.out.println("[DEBUG] Submission ID: " + submissionId);

            // Problem link → extract contestId and problemId
            // href is like /contest/1234/problem/A  or  /problemset/problem/1234/A
            String problemHref = "";
            String problemName = "";
            String contestId   = "";
            String problemId   = "";
            
            // Try to find problem link - first approach: td.problem-cell a
            List<WebElement> problemLinks = row.findElements(By.cssSelector("td.problem-cell a"));
            if (!problemLinks.isEmpty()) {
                WebElement problemLink = problemLinks.get(0);
                problemName = problemLink.getText().trim();
                problemHref = problemLink.getAttribute("href");
                System.out.println("[DEBUG] Problem: " + problemName + " | href: " + problemHref);
            } else {
                // fallback: any link in the row that looks like a problem
                System.out.println("[DEBUG] Problem cell not found, using fallback...");
                for (WebElement a : row.findElements(By.tagName("a"))) {
                    String href = a.getAttribute("href");
                    if (href != null && href.contains("/problem/")) {
                        problemHref = href;
                        problemName = a.getText().trim();
                        System.out.println("[DEBUG] Found via fallback: " + problemName + " | href: " + problemHref);
                        break;
                    }
                }
            }

            // Parse contest ID from href
            if (problemHref.contains("/contest/")) {
                String[] parts = problemHref.split("/");
                for (int i = 0; i < parts.length - 1; i++) {
                    if ("contest".equals(parts[i])) {
                        contestId = parts[i + 1];
                        break;
                    }
                }
                // Problem ID = last part of the URL (e.g. "A", "B", "C1")
                if (!problemHref.isEmpty()) {
                    String[] urlParts = problemHref.split("/");
                    problemId = urlParts[urlParts.length - 1].split("\\?")[0];
                }
                System.out.println("[DEBUG] Parsed: contestId=" + contestId + ", problemId=" + problemId);
            } else if (problemHref.contains("/problemset/problem/")) {
                String[] parts = problemHref.split("/");
                for (int i = 0; i < parts.length - 1; i++) {
                    if ("problem".equals(parts[i])) {
                        contestId = parts[i + 1];
                        problemId = (i + 2 < parts.length) ? parts[i + 2].split("\\?")[0] : "";
                        break;
                    }
                }
                System.out.println("[DEBUG] Parsed problemset: contestId=" + contestId + ", problemId=" + problemId);
            } else {
                System.out.println("[DEBUG] ⚠️  Không nhận diện problemHref format: " + problemHref);
            }

            // Language: look for td that contains programming language text
            String language = "";
            try {
                List<WebElement> tds = row.findElements(By.tagName("td"));
                System.out.println("[DEBUG] Row có " + tds.size() + " columns");
                // Language is typically the 5th column (index 4)
                if (tds.size() > 4) language = tds.get(4).getText().trim();
                System.out.println("[DEBUG] Language: " + language);
            } catch (Exception ignored) {}

            SubmissionMeta m = new SubmissionMeta();
            m.submissionId = submissionId;
            m.contestId    = contestId;
            m.problemId    = problemId.toUpperCase();
            m.problemName  = problemName;
            m.language     = language;
            System.out.println("[DEBUG] ✅ Created SubmissionMeta: id=" + submissionId + ", problem=" + problemName);
            return m;

        } catch (Exception e) {
            System.out.println("[DEBUG] ❌ Exception in parseRow: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Step 4 helper: open submission detail page and extract source code
    // ──────────────────────────────────────────────────────────────────────────

    private String fetchSourceCode(WebDriver driver, WebDriverWait wait, SubmissionMeta m)
            throws InterruptedException {

        String url = buildSubmissionUrl(m);
        driver.get(url);

        try {
            // Wait for the source code element to appear
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(SEL_SOURCE_CODE)));
            WebElement pre = driver.findElement(By.cssSelector(SEL_SOURCE_CODE));

            // Sometimes the element is hidden behind a "View source" button – click it
            if (!pre.isDisplayed()) {
                try {
                    WebElement viewBtn = driver.findElement(
                            By.xpath("//a[contains(text(),'View source') or contains(text(),'Посмотреть исходный')]"));
                    viewBtn.click();
                    Thread.sleep(800);
                    pre = driver.findElement(By.cssSelector(SEL_SOURCE_CODE));
                } catch (Exception ignored) {}
            }

            // Use JS to get the full text (avoids truncation for large files)
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript("return arguments[0].innerText;", pre);
            return result != null ? result.toString() : pre.getText();

        } catch (Exception e) {
            return null;
        }
    }

    private String buildSubmissionUrl(SubmissionMeta m) {
        if (m.contestId == null || m.contestId.isBlank()) {
            // Fallback: use the submissions list link (won't have source but won't crash)
            return "https://codeforces.com/submission/" + m.submissionId;
        }
        // Gym contests have IDs >= 100000
        try {
            long cid = Long.parseLong(m.contestId);
            if (cid >= 100_000) {
                return "https://codeforces.com/gym/" + cid + "/submission/" + m.submissionId;
            }
        } catch (NumberFormatException ignored) {}
        return "https://codeforces.com/contest/" + m.contestId + "/submission/" + m.submissionId;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Driver factory
    // ──────────────────────────────────────────────────────────────────────────

    private WebDriver buildDriver() {
        ChromeOptions options = new ChromeOptions();

        String profilePath = cfg.chromeProfilePath();
        String profileDir  = cfg.chromeProfileDir();

        if (profilePath != null && !profilePath.isBlank()) {
            options.addArguments("user-data-dir=" + profilePath);
            options.addArguments("profile-directory=" + profileDir);
            System.out.println("[Crawler] Dùng Chrome profile: " + profilePath + " / " + profileDir);
        } else {
            System.out.println("[Crawler] Không có Chrome profile, dùng profile mới.");
        }

        // Reduce bot-detection signals (only safe flags)
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--start-maximized");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        try {
            ChromeDriver driver = new ChromeDriver(options);
            // JS override to hide webdriver flag
            ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
            return driver;
        } catch (Exception e) {
            System.err.println("[Crawler] ❌ Không mở được Chrome: " + e.getMessage());
            throw e;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Block the crawler thread until the user clicks OK in the Swing dialog
    // Uses invokeAndWait → EDT shows dialog → crawler thread blocks cleanly
    // ──────────────────────────────────────────────────────────────────────────

    private void waitForOkDialog(String username) throws InterruptedException {
        try {
            javax.swing.SwingUtilities.invokeAndWait(() ->
                javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Chrome đã mở trang bài nộp của \"" + username + "\".\n\n"
                    + "LƯU Ý QUAN TRỌNG TRƯỚC KHI BẤM OK:\n"
                    + "1. Vượt qua Cloudflare (nhấn 'I am human') nếu có.\n"
                    + "2. ĐĂNG NHẬP CODEFORCES TẠI TRÌNH DUYỆT NÀY nếu bạn đang cào bài của chính mình,\n"
                    + "   bài trong Gym, hoặc Group kín (để có quyền xem Source Code).\n"
                    + "3. Chờ trang tải xong danh sách bài nộp rồi mới nhấn OK tại đây.",
                    "⏳ Chờ bạn thiết lập & đăng nhập...",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                )
            );
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new RuntimeException("Dialog error", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Inner types
    // ──────────────────────────────────────────────────────────────────────────

    /** Lightweight DTO to hold submission metadata scraped from the list page. */
    private static class SubmissionMeta {
        String submissionId;
        String contestId;
        String problemId;
        String problemName;
        String language;
    }

    /** Callback interface so the UI can display crawl progress in real time. */
    public interface CrawlStatusCallback {
        void onStatus(String message);
    }

    private void log(CrawlStatusCallback cb, String msg) {
        System.out.println("[Crawler] " + msg);
        if (cb != null) cb.onStatus(msg);
    }
}
