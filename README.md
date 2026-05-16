# Code Analyzer System

Ung dung Java Swing dung de crawl bai nop Codeforces, phan tich source code bang Gemini AI va danh gia tong quan tung sinh vien.

## Yeu cau

- Java JDK 21+
- Maven
- MySQL Server 5.7+ hoac 8.x
- Google Chrome
- Gemini API key

## Cau hinh

Mo `src/main/resources/config.properties` va sua cac gia tri sau:

```properties
db.url=jdbc:mysql://localhost:3306/code_analyzer?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=root
db.password=YOUR_MYSQL_PASSWORD

gemini.api.key=YOUR_GEMINI_API_KEY
chrome.profile.path=C:/Users/YOUR_USER/AppData/Local/CodeAnalyzer_Chrome
```

Chu y: chuong trinh khong tu tao database/table. Hay tao database va cac bang truoc khi chay app.

## Chay chuong trinh

```bash
mvn compile
mvn exec:java
```

Hoac build file jar:

```bash
mvn package
java -jar target/code-analyzer-1.0-SNAPSHOT.jar
```

## Tinh nang chinh

- Quan ly danh sach username Codeforces.
- Crawl submissions bang Selenium voi Chrome profile rieng.
- Phan tich source code bang Gemini.
- Luu ket qua vao MySQL.
- Danh gia tong quan sinh vien theo DSA score va AI dependency score.
- Lap lich crawl/phan tich/danh gia dinh ky.

## Cau truc source

```text
src/main/java/com/codeanalyzer/ai         GeminiService, CodeAnalyzer, StudentEvaluator
src/main/java/com/codeanalyzer/crawler    Selenium crawler
src/main/java/com/codeanalyzer/database   DatabaseConnection va DAO
src/main/java/com/codeanalyzer/model      Model classes
src/main/java/com/codeanalyzer/scheduler  CrawlScheduler
src/main/java/com/codeanalyzer/ui         Swing UI
src/main/java/com/codeanalyzer/util       AppConfig, HttpUtil, JsonUtil
src/main/resources/config.properties      Local config
```

## Bao mat

Khong commit API key Gemini hoac password MySQL that len GitHub. File `.gitignore` da bo qua `config.properties`, `target/`, `bin/`, file `.class` va file `.jar`.
