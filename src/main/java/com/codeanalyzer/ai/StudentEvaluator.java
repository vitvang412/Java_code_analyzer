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
 *  - dsaScore (0-10)         : dựa trên số loại CTDL / thuật toán khác nhau mà SV dùng.
 *                              Ta quy đổi số loại unique → điểm (max khi >= 10 loại).
 *  - aiDependencyScore (0-10): trung bình ai_usage_score của các bài đã phân tích.
 *  - topAlgorithms / topDS   : top 5 theo tần suất, format "Tên (x lần)".
 */
public class StudentEvaluator {

    private final AnalysisResultDAO      analysisResultDAO     = new AnalysisResultDAO();
    private final StudentEvaluationDAO   studentEvaluationDAO  = new StudentEvaluationDAO();
    private final StudentDAO             studentDAO            = new StudentDAO();

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
        for (AnalysisResult r : results) sumAi += r.getAiUsageScore();
        double avgAi = sumAi / results.size();

        // ── Gom tần suất các thuật toán / CTDL ──────────────────────────────
        Map<String, Integer> algoCount = new HashMap<>();
        Map<String, Integer> dsCount   = new HashMap<>();
        for (AnalysisResult r : results) {
            addAllFromJsonArray(r.getAlgorithms(),      algoCount);
            addAllFromJsonArray(r.getDataStructures(),  dsCount);
        }

        // ── DSA score: càng đa dạng càng cao, max 10 khi >= 10 loại ─────────
        int uniqueAlgo = algoCount.size();
        int uniqueDs   = dsCount.size();
        // 60% từ algorithm, 40% từ data structure
        double dsaScore = Math.min(10.0,
                0.6 * Math.min(10, uniqueAlgo) + 0.4 * Math.min(10, uniqueDs));

        eval.setDsaScore(round2(dsaScore));
        eval.setAiDependencyScore(round2(avgAi));
        eval.setTopAlgorithms(top(algoCount, 5));
        eval.setTopDataStructures(top(dsCount, 5));

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
                // Bỏ các từ quá generic để thống kê có ý nghĩa hơn
                String lower = v.toLowerCase();
                if (lower.equals("array") || lower.equals("mảng") || lower.equals("variable")) continue;
                counter.merge(v, 1, Integer::sum);
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
