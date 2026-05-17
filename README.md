# Hệ Thống Code Analyzer

Ứng dụng Java Swing dùng để crawl bài nộp Codeforces, phân tích mã nguồn bằng Gemini AI và đánh giá tổng quan từng sinh viên.

## Yêu Cầu

- Java JDK 21+
- Maven
- MySQL Server 5.7+ hoặc 8.x
- Google Chrome
- Gemini API key

## Cấu Hình

Mở file `src/main/resources/config.properties` và sửa các giá trị sau:

```properties
db.url=jdbc:mysql://localhost:3306/code_analyzer?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=root
db.password=YOUR_MYSQL_PASSWORD

gemini.api.key=YOUR_GEMINI_API_KEY
chrome.profile.path=C:/Users/YOUR_USER/AppData/Local/CodeAnalyzer_Chrome
```

Có thể đặt biến môi trường `DB_PASSWORD`, `GEMINI_API_KEY` và `CHROME_PROFILE_PATH` để chạy local mà không cần ghi thông tin nhạy cảm trực tiếp vào file cấu hình.

Lưu ý: Chương trình không tự tạo database/table. Hãy tạo database và các bảng trước khi chạy ứng dụng.

## Chạy Chương Trình

```bash
mvn compile
mvn exec:java
```

Hoặc build file JAR:

```bash
mvn package
java -jar target/code-analyzer-1.0-SNAPSHOT.jar
```

## Tính Năng Chính

- Quản lý danh sách username Codeforces.
- Crawl submissions bằng Selenium với Chrome profile riêng.
- Phân tích source code bằng Gemini AI.
- Lưu kết quả vào MySQL.
- Đánh giá tổng quan sinh viên theo DSA Score và AI Dependency Score.
- Lập lịch crawl, phân tích và đánh giá định kỳ.

## Cấu Trúc Source

```text
src/main/java/com/codeanalyzer/ai         GeminiService, CodeAnalyzer, StudentEvaluator
src/main/java/com/codeanalyzer/crawler    Selenium crawler
src/main/java/com/codeanalyzer/database   DatabaseConnection và DAO
src/main/java/com/codeanalyzer/model      Model classes
src/main/java/com/codeanalyzer/scheduler  CrawlScheduler
src/main/java/com/codeanalyzer/ui         Swing UI
src/main/java/com/codeanalyzer/util       AppConfig, HttpUtil, JsonUtil
src/main/resources/config.properties      File cấu hình local
```

## Bảo Mật

Không commit Gemini API key hoặc mật khẩu MySQL thật lên GitHub. File `.gitignore` đã bỏ qua `config.properties`, `target/`, `bin/`, file `.class` và file `.jar`.
