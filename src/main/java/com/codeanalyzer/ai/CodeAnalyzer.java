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
 *   complexity_estimate, summary, code_quality, naming_style, logic_pattern,
 *   strengths, weaknesses, ai_evidence.
 */
public class CodeAnalyzer {
    private final GeminiService       geminiService     = new GeminiService();
    private final AnalysisResultDAO   analysisResultDAO = new AnalysisResultDAO();

    public AnalysisResult analyzeSubmission(Submission submission) {
        System.out.println("[Analyze] Đang phân tích bài nộp ID: " + submission.getId());

        String prompt = """
            Bạn là một chuyên gia phân tích mã nguồn lập trình thi đấu, có khả năng phân biệt code của học sinh tự viết và code được tạo bởi AI (ChatGPT, Claude, Copilot...).

            Ngôn ngữ lập trình: %s
            Tên bài toán: %s
            Mã nguồn:
            ```
            %s
            ```

            Hãy phân tích CẨN THẬN và KỸ LƯỠNG rồi trả về DUY NHẤT 1 object JSON (không markdown, không comment ngoài JSON).
            Các trường JSON bắt buộc:

            === CẤU TRÚC DỮ LIỆU & THUẬT TOÁN ===
              "data_structures": mảng chuỗi – Tất cả CTDL được dùng (ví dụ ["HashMap", "Stack", "Array"]).
              "algorithms": mảng chuỗi – Tất cả thuật toán/kỹ thuật (ví dụ ["Dijkstra", "Dynamic Programming", "Greedy"]).
              "complexity_estimate": chuỗi – Độ phức tạp thời gian + không gian (ví dụ "O(N log N) time, O(N) space").

            === ĐÁNH GIÁ AI USAGE (0–10) ===
              "ai_usage_score": số nguyên 0–10:
                0–2  = Tự viết rõ ràng: code thô, tên biến cộc lốc, debug nhiều, logic không nhất quán.
                3–4  = Chủ yếu tự viết: có style riêng nhưng cũng tham khảo tài liệu.
                5–6  = Trung bình: code khá sạch nhưng vẫn có dấu vết cá nhân.
                7–8  = Có dấu hiệu AI mạnh: comment tiếng Anh hoàn hảo, tên hàm quá rõ nghĩa, cấu trúc template điển hình.
                9–10 = Gần chắc AI tạo: quá hoàn hảo, over-engineered, mọi hàm đều có docstring, style nhất quán tuyệt đối.
              "ai_usage_reason": chuỗi tiếng Việt CHI TIẾT (3–5 câu) giải thích bằng chứng cụ thể: style code, comment, naming, logic pattern, điểm bất thường.

            === CHẤT LƯỢNG CODE ===
              "code_quality": số nguyên 1–10 – chất lượng tổng thể (độ rõ ràng, tổ chức code, tránh trùng lặp).
              "naming_style": chuỗi tiếng Việt – nhận xét về cách đặt tên biến/hàm (ví dụ: "Đặt tên ngắn gọn kiểu thi đấu (a, b, ans)", "Tên có nghĩa rõ ràng (totalCost, minDistance)").
              "logic_pattern": chuỗi tiếng Việt – mô tả cấu trúc tổng thể của giải pháp (ví dụ: "Đọc input → build graph → chạy BFS → in kết quả").

            === NHẬN XÉT CHI TIẾT ===
              "strengths": mảng chuỗi tiếng Việt – Tối đa 3 điểm mạnh cụ thể của code (kỹ thuật, hiệu quả, logic).
              "weaknesses": mảng chuỗi tiếng Việt – Tối đa 3 điểm yếu hoặc vấn đề tiềm ẩn.
              "ai_evidence": mảng chuỗi tiếng Việt – Các bằng chứng cụ thể ủng hộ (hoặc bác bỏ) giả thuyết AI, ví dụ ["Biến tạm được đặt tên rất gợi nghĩa (leftBound, rightBound)", "Comment giải thích từng bước rất chi tiết"].

            === TÓM TẮT ===
              "summary": 2–3 câu tiếng Việt tóm tắt: hướng giải, độ phức tạp, và nhận định chung về code.
            """.formatted(
                submission.getLanguage(),
                submission.getProblemName() != null ? submission.getProblemName() : "Không rõ",
                submission.getSourceCode());

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
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
                result.setSummary(buildEnrichedSummary(resultJson));  // tổng hợp các trường mới

                analysisResultDAO.save(result);
                System.out.println("[Analyze] ✅ Xong bài ID: " + submission.getId());
                return result;

            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                boolean isTransientError = errorMsg.contains("high demand")
                                        || errorMsg.contains("503")
                                        || errorMsg.contains("malformedjsonexception")
                                        || errorMsg.contains("unterminated string")
                                        || errorMsg.contains("timeout")
                                        || errorMsg.contains("429");

                if (isTransientError && attempt < maxRetries) {
                    System.err.println("[Analyze] ⚠ Lỗi tạm thời. Đợi 10s rồi thử lại lần " + (attempt + 1) + "...");
                    try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
                    continue;
                }

                System.err.println("[Analyze] ❌ Lỗi bài ID " + submission.getId() + ": " + e.getMessage());
                throw new RuntimeException("Gemini phân tích thất bại: " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Kết hợp summary gốc + naming_style + logic_pattern + strengths/weaknesses thành đoạn tóm tắt phong phú,
     * lưu vào cột summary (backward-compatible).
     */
    private String buildEnrichedSummary(JsonObject j) {
        StringBuilder sb = new StringBuilder();

        String summary = JsonUtil.getString(j, "summary");
        if (summary != null && !summary.isBlank()) sb.append(summary).append("\n\n");

        String logicPattern = JsonUtil.getString(j, "logic_pattern");
        if (logicPattern != null && !logicPattern.isBlank())
            sb.append("📐 Cấu trúc giải pháp: ").append(logicPattern).append("\n");

        String namingStyle = JsonUtil.getString(j, "naming_style");
        if (namingStyle != null && !namingStyle.isBlank())
            sb.append("🏷️ Phong cách đặt tên: ").append(namingStyle).append("\n");

        int codeQuality = JsonUtil.getInt(j, "code_quality", -1);
        if (codeQuality >= 0)
            sb.append("⭐ Chất lượng code: ").append(codeQuality).append("/10\n");

        // Strengths
        if (j.has("strengths") && j.get("strengths").isJsonArray()) {
            sb.append("\n✅ Điểm mạnh:\n");
            j.getAsJsonArray("strengths").forEach(el ->
                sb.append("  • ").append(el.getAsString()).append("\n"));
        }

        // Weaknesses
        if (j.has("weaknesses") && j.get("weaknesses").isJsonArray()) {
            sb.append("\n⚠️ Điểm cần cải thiện:\n");
            j.getAsJsonArray("weaknesses").forEach(el ->
                sb.append("  • ").append(el.getAsString()).append("\n"));
        }

        // AI Evidence
        if (j.has("ai_evidence") && j.get("ai_evidence").isJsonArray()) {
            sb.append("\nBằng chứng AI:\n");
            j.getAsJsonArray("ai_evidence").forEach(el ->
                sb.append("  • ").append(el.getAsString()).append("\n"));
        }

        return sb.toString().trim();
    }
}
