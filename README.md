# Code Analyzer System (Semi-Auto Edition)

Hệ thống phân tích mã nguồn sinh viên từ Codeforces sử dụng **Gemini AI** +
**scheduler tự động**.

## 🚀 Tính năng nổi bật
- **Cào dữ liệu bán tự động (Semi-auto):** Vượt qua Cloudflare bằng cách để
  người dùng xác nhận "I am human" trên Chrome, sau đó Selenium tự cào tiếp.
- **Phân tích bằng Gemini 1.5 Flash:** đọc hiểu code, chấm điểm nghi ngờ dùng
  AI, liệt kê CTDL / thuật toán / độ phức tạp / tóm tắt hướng giải.
- **Đánh giá tổng thể sinh viên:** tính `dsa_score` (mức độ đa dạng CTDL / thuật
  toán), `ai_dependency_score` (trung bình điểm nghi ngờ AI), top thuật toán,
  top CTDL, và nhãn xếp hạng `Tốt / Trung bình / Yếu`.
- **Lập lịch tự động (24h mặc định):** chu trình **Crawl → Phân tích → Đánh
  giá** cho toàn bộ nick đang hoạt động. Có nút *Bật / Tắt / Chạy ngay* ngay
  trong UI.
- **Giao diện Swing hiện đại:** gradient header, nút bo tròn, bảng đánh giá có
  tô màu theo score, status bar hiển thị trạng thái scheduler realtime.

## 🛠 Yêu cầu hệ thống
1. **Java JDK 21** (dự án đã được cấu hình target Java 21 để hết warning khi
   dùng JDK 21). Nếu bạn vẫn chỉ có JDK 17, đổi `pom.xml` về `release=17`.
2. **MySQL Server** 5.7+ hoặc 8.x.
3. **Google Chrome** (Selenium Manager sẽ tự tải driver phù hợp).
4. **Thư viện (Jar) trong `lib/`**:
   - `gson-2.10.1.jar`
   - `jsoup-1.17.2.jar`
   - `mysql-connector-j-8.0.33.jar`
   - `selenium-server-4.x.x.jar` (và các dependency đi kèm)

## ⚙ Cấu hình
Mở file `src/main/resources/config.properties` và cập nhật:
- `db.password`: mật khẩu MySQL của bạn.
- `gemini.api.key`: API key Gemini (lấy tại
  [Google AI Studio](https://aistudio.google.com/apikey)).
- `chrome.profile.path`: đường dẫn Chrome profile để Selenium lưu cookie.
- `crawler.schedule.hours`: chu kỳ chạy scheduler mặc định (giờ). Bạn có thể
  đổi trong tab **Lịch Tự Động** mà không cần sửa file.

> ⚠️ **Bảo mật:** không commit `gemini.api.key` thật hoặc password MySQL thật
> lên git. Dùng file local đã được `.gitignore` nếu repo của bạn đã cấu hình.

## 🖱 Cách vận hành

### A. Cào + phân tích thủ công (mỗi lần 1 nick)
1. Đảm bảo đã đóng Chrome nếu đang dùng profile mặc định.
2. Chạy `run.bat` hoặc `java -cp "bin;src/main/resources;lib/*" com.codeanalyzer.Main`.
3. Tab **Quản lý Nick** → thêm username Codeforces → chọn dòng → nhấn *Bắt đầu
   cào (Semi-Auto)*.
4. Chrome mở trang submissions. Nếu thấy Cloudflare → nhấn *I am human*.
5. Khi thấy bảng bài nộp, quay lại ứng dụng và nhấn **OK** ở hộp thoại đang
   chờ.
6. Sau khi cào xong → mở tab **Phân Tích AI** → nhấn *Bắt đầu phân tích*.
7. Mở tab **Đánh Giá** → nhấn *Đánh giá lại tất cả* để tổng hợp điểm.

### B. Chạy tự động định kỳ (khuyến nghị)
1. Mở tab **Lịch Tự Động**.
2. Đặt *Chu kỳ (giờ)* – mặc định 24.
3. Nhấn **▶ Bật lịch**.
4. Mỗi chu kỳ, hệ thống sẽ tuần tự cho từng nick: crawl mới → Gemini phân tích
   các bài chưa có analysis → tổng hợp lại `student_evaluations`.
5. Vì crawl là semi-auto, bạn vẫn cần có mặt để nhấn "I am human" khi
   Cloudflare xuất hiện. Nếu muốn bỏ qua 1 nick, dừng lịch và crawl tay.
6. Có thể bấm **⚡ Chạy ngay** để chạy thử 1 chu trình bất kỳ lúc nào.

## 📁 Cấu trúc dự án
```
com.codeanalyzer.crawler    – Selenium crawl Codeforces (semi-auto)
com.codeanalyzer.ai         – GeminiService, CodeAnalyzer, StudentEvaluator
com.codeanalyzer.scheduler  – CrawlScheduler (singleton, ScheduledExecutor)
com.codeanalyzer.database   – DAO + DatabaseInitializer (4 bảng)
com.codeanalyzer.model      – POJOs (Student, Submission, AnalysisResult,
                              StudentEvaluation, PlatformType)
com.codeanalyzer.ui         – MainFrame, panels, components (StyledButton,
                              StatusBar)
com.codeanalyzer.util       – AppConfig, HttpUtil, JsonUtil
```

## 🗃 Các bảng DB
| Bảng | Mô tả |
|---|---|
| `students` | Danh sách nick được theo dõi |
| `submissions` | Các bài nộp đã crawl (source code + verdict) |
| `analysis_results` | Kết quả Gemini cho từng bài nộp |
| `student_evaluations` | Tổng hợp đánh giá theo sinh viên |

## 🧪 Build & chạy bằng CLI
```bash
# Biên dịch
javac --release 21 -d bin -cp "lib/*" $(find src/main/java -name "*.java")

# Chạy
java -cp "bin:src/main/resources:lib/*" com.codeanalyzer.Main
```

(Trên Windows dùng `;` thay cho `:` và file `run.bat` đã có sẵn.)
