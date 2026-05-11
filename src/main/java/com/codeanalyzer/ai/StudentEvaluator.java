package com.codeanalyzer.ai;

import com.codeanalyzer.database.AnalysisResultDAO;
import com.codeanalyzer.database.StudentDAO;
import com.codeanalyzer.database.StudentEvaluationDAO;
import com.codeanalyzer.model.AnalysisResult;
import com.codeanalyzer.model.Student;
import com.codeanalyzer.model.StudentEvaluation;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tổng hợp đánh giá một sinh viên dựa trên tất cả analysis_results đã có.
 *
 * Điểm tính:
 *  - dsaScore (0-10)         : 60% từ số loại thuật toán unique, 40% từ CTDL unique
 *  - aiDependencyScore (0-10): trung bình ai_usage_score của các bài đã phân tích
 *  - topAlgorithms / topDS   : top 5 theo tần suất, format "Tên (x lần)"
 */
public class StudentEvaluator {

    private final AnalysisResultDAO    analysisResultDAO    = new AnalysisResultDAO();
    private final StudentEvaluationDAO studentEvaluationDAO = new StudentEvaluationDAO();
    private final StudentDAO           studentDAO           = new StudentDAO();

    /** Đánh giá 1 sinh viên và LƯU vào bảng student_evaluations. Trả về object kết quả. */
    public StudentEvaluation evaluateStudent(Student student) {
        List<AnalysisResult> results = analysisResultDAO.findByStudentId(student.getId());

        StudentEvaluation eval = new StudentEvaluation();
        eval.setStudentId(student.getId());
        eval.setStudentUsername(student.getUsername());
        eval.setTotalAnalyzed(results.size());

        if (results.isEmpty()) {
            eval.setDsaScore(0);
            eval.setAiDependencyScore(0);
            eval.setTopAlgorithms("");
            eval.setTopDataStructures("");
            studentEvaluationDAO.save(eval);
            return eval;
        }

        // ── AI dependency: trung bình ai_usage_score ────────────────────────
        double sumAi = 0;
        int highAiCount = 0; // bài bị nghi dùng AI cao (>= 7)
        for (AnalysisResult r : results) {
            int score = r.getAiUsageScore();
            sumAi += score;
            if (score >= 7) highAiCount++;
        }
        double avgAi = sumAi / results.size();

        // ── Gom tần suất các thuật toán / CTDL ──────────────────────────────
        Map<String, Integer> algoCount = new HashMap<>();
        Map<String, Integer> dsCount   = new HashMap<>();
        Set<String> complexities       = new LinkedHashSet<>();
        for (AnalysisResult r : results) {
            addAllFromJsonArray(r.getAlgorithms(),     algoCount);
            addAllFromJsonArray(r.getDataStructures(), dsCount);
            if (r.getComplexityEstimate() != null && !r.getComplexityEstimate().isBlank()) {
                complexities.add(r.getComplexityEstimate().trim());
            }
        }

        // ── DSA score: càng đa dạng càng cao, max 10 khi >= 10 loại ─────────
        int uniqueAlgo = algoCount.size();
        int uniqueDs   = dsCount.size();
        double dsaScore = Math.min(10.0,
                0.6 * Math.min(10, uniqueAlgo) + 0.4 * Math.min(10, uniqueDs));

        // ── Overall summary (nhận xét tổng hợp bằng tiếng Việt) ─────────────
        String overallSummary = buildSummary(
                results.size(), dsaScore, avgAi, highAiCount,
                uniqueAlgo, uniqueDs, complexities,
                top(algoCount, 3), top(dsCount, 3));

        eval.setDsaScore(round2(dsaScore));
        eval.setAiDependencyScore(round2(avgAi));
        eval.setTopAlgorithms(top(algoCount, 5));
        eval.setTopDataStructures(top(dsCount, 5));
        eval.setOverallSummary(overallSummary);

        studentEvaluationDAO.save(eval);
        return eval;
    }

    /** Tiện lợi khi chỉ có id. */
    public StudentEvaluation evaluateStudent(int studentId) {
        Student s = studentDAO.findById(studentId);
        if (s == null) return null;
        return evaluateStudent(s);
    }

    /** Đánh giá toàn bộ sinh viên đang active. */
    public List<StudentEvaluation> evaluateAll() {
        List<StudentEvaluation> out = new ArrayList<>();
        for (Student s : studentDAO.findAll()) {
            out.add(evaluateStudent(s));
        }
        return out;
    }

    // ────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────

    /** Xây dựng đoạn nhận xét tổng hợp bằng tiếng Việt. */
    private String buildSummary(int total, double dsaScore, double avgAi,
                                int highAiCount, int uniqueAlgo, int uniqueDs,
                                Set<String> complexities, String topAlgo, String topDs) {
        StringBuilder sb = new StringBuilder();

        // Đánh giá trình độ DSA
        sb.append("[CTDL & Thuật toán] ");
        if (dsaScore >= 8) {
            sb.append("Xuất sắc – sử dụng rất đa dạng (").append(uniqueAlgo)
              .append(" thuật toán, ").append(uniqueDs).append(" CTDL). ");
        } else if (dsaScore >= 5) {
            sb.append("Khá tốt – đa dạng vừa phải (").append(uniqueAlgo)
              .append(" thuật toán, ").append(uniqueDs).append(" CTDL). ");
        } else if (dsaScore >= 2) {
            sb.append("Trung bình – chủ yếu dùng ").append(topAlgo).append(". ");
        } else {
            sb.append("Yếu – chưa đa dạng, cần bổ sung CTDL và thuật toán. ");
        }
        if (!topAlgo.isBlank()) sb.append("Hay dùng: ").append(topAlgo).append(". ");
        if (!topDs.isBlank())   sb.append("CTDL: ").append(topDs).append(". ");

        // Độ phức tạp
        if (!complexities.isEmpty()) {
            sb.append("Complexity gặp: ").append(String.join(", ", complexities)).append(". ");
        }

        // Đánh giá AI
        sb.append("[Mức độ AI] ");
        if (avgAi <= 2) {
            sb.append("Rất thấp – hầu hết tự viết (avg ").append(round2(avgAi)).append("/10). ");
        } else if (avgAi <= 4) {
            sb.append("Thấp – phần lớn tự viết (avg ").append(round2(avgAi)).append("/10). ");
        } else if (avgAi <= 6) {
            sb.append("Trung bình – một số bài dùng AI (avg ").append(round2(avgAi)).append("/10). ");
        } else if (avgAi <= 8) {
            sb.append("Cao – nhiều bài nghi dùng AI (").append(highAiCount).append("/").append(total)
              .append(" bài, avg ").append(round2(avgAi)).append("/10). ");
        } else {
            sb.append("Rất cao – hầu hết do AI tạo (").append(highAiCount).append("/").append(total)
              .append(" bài, avg ").append(round2(avgAi)).append("/10). ");
        }
        sb.append("Tổng phân tích: ").append(total).append(" bài.");
        return sb.toString();
    }

    /** Đọc chuỗi JSON array (["BFS", "DFS"]) và cộng tần suất vào map. */
    private void addAllFromJsonArray(String jsonArrayStr, Map<String, Integer> counter) {
        if (jsonArrayStr == null || jsonArrayStr.isBlank()) return;
        try {
            JsonElement el = JsonParser.parseString(jsonArrayStr);
            if (!el.isJsonArray()) return;
            JsonArray arr = el.getAsJsonArray();
            for (JsonElement item : arr) {
                if (item == null || item.isJsonNull()) continue;
                String v = item.isJsonPrimitive() ? item.getAsString() : item.toString();
                v = v == null ? "" : v.trim();
                if (v.isEmpty()) continue;
                String lower = v.toLowerCase();
                if (lower.equals("array") || lower.equals("mảng") || lower.equals("variable")) continue;
                // Dùng lambda tường minh để tránh null-safety warning
                counter.merge(v, 1, (a, b) -> a + b);
            }
        } catch (Exception ignored) {
            // Nếu không parse được (Gemini trả về text thô), bỏ qua
        }
    }

    private String top(Map<String, Integer> counter, int n) {
        if (counter.isEmpty()) return "";
        return counter.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(n)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining(", "));
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}