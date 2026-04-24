package com.codeanalyzer.ai;

import com.codeanalyzer.database.AnalysisResultDAO;
import com.codeanalyzer.model.AnalysisResult;
import com.codeanalyzer.model.Submission;
import com.codeanalyzer.util.JsonUtil;
import com.google.gson.JsonObject;

/**
 * Dùng Gemini phân tích 1 bài nộp và lưu kết quả vào bảng analysis_results.
 *
 * Prompt được thiết kế để Gemini trả về JSON duy nhất với các trường:
 *   data_structures, algorithms, ai_usage_score, ai_usage_reason,
 *   complexity_estimate, summary.
 */
public class CodeAnalyzer {
    private final GeminiService       geminiService     = new GeminiService();
    private final AnalysisResultDAO   analysisResultDAO = new AnalysisResultDAO();

    public AnalysisResult analyzeSubmission(Submission submission) {
        System.out.println("[Analyze] Đang phân tích bài nộp ID: " + submission.getId());

        String prompt = """
            Bạn là một chuyên gia chấm bài lập trình thi đấu.
            Nhiệm vụ: đọc đoạn mã nguồn dưới đây và đánh giá.

            Ngôn ngữ: %s
            Source code:
            ```
            %s
            ```

            Hãy trả về DUY NHẤT 1 object JSON (không markdown, không giải thích ngoài JSON) với các trường sau:
              - "data_structures": mảng chuỗi các CTDL chính được dùng (ví dụ ["HashMap", "Stack"]).
              - "algorithms": mảng chuỗi các thuật toán/kỹ thuật chính (ví dụ ["Dijkstra", "Dynamic Programming"]).
              - "ai_usage_score": số nguyên 0–10 thể hiện mức nghi ngờ code sinh bởi AI.
                  0 = rõ ràng tự viết (style cá nhân, đặt tên không đồng đều, có dấu vết debug...).
                  10 = gần như chắc chắn do AI tạo (chú thích quá chuẩn chỉnh, đặt tên rất nhất quán,
                       cấu trúc quá sạch, sử dụng tính năng hiếm gặp không cần thiết, over-engineered...).
                  Cân nhắc: độ đồng đều về code style, mật độ comment tiếng Anh chuẩn,
                  pattern boilerplate đặc trưng AI, sự "hoàn hảo" không tự nhiên.
              - "ai_usage_reason": chuỗi tiếng Việt ngắn gọn, giải thích lý do cho điểm trên.
              - "complexity_estimate": độ phức tạp thời gian ước tính (ví dụ "O(N log N)").
              - "summary": 1-2 câu tiếng Việt tóm tắt hướng giải.
            """.formatted(submission.getLanguage(), submission.getSourceCode());

        try {
            String jsonResponse = geminiService.generate(prompt);

            // Loại bỏ markdown fences nếu Gemini có bao ngoài
            jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();

            JsonObject resultJson = JsonUtil.parseObject(jsonResponse);

            AnalysisResult result = new AnalysisResult();
            result.setSubmissionId(submission.getId());
            result.setDataStructures(JsonUtil.toJson(resultJson.get("data_structures")));
            result.setAlgorithms(JsonUtil.toJson(resultJson.get("algorithms")));
            result.setAiUsageScore(JsonUtil.getInt(resultJson, "ai_usage_score", 0));
            result.setAiUsageReason(JsonUtil.getString(resultJson, "ai_usage_reason"));
            result.setComplexityEstimate(JsonUtil.getString(resultJson, "complexity_estimate"));
            result.setSummary(JsonUtil.getString(resultJson, "summary"));

            analysisResultDAO.save(result);
            System.out.println("[Analyze] ✅ Xong bài ID: " + submission.getId());
            return result;

        } catch (Exception e) {
            System.err.println("[Analyze] ❌ Lỗi bài ID " + submission.getId() + ": " + e.getMessage());
            return null;
        }
    }
}
