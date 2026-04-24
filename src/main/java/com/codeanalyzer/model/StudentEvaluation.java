package com.codeanalyzer.model;

import java.time.LocalDateTime;

/**
 * Tổng hợp đánh giá cho 1 sinh viên (dựa trên toàn bộ analysis_results của họ).
 *
 *  - dsaScore          : 0-10, mức độ đa dạng CTDL / thuật toán (càng cao càng tốt)
 *  - aiDependencyScore : 0-10, mức độ nghi ngờ dùng AI (càng cao càng xấu)
 *  - topAlgorithms     : chuỗi các thuật toán phổ biến nhất, vd "DP (5), BFS (3)"
 *  - topDataStructures : chuỗi các CTDL phổ biến nhất, vd "HashMap (4), ArrayList (2)"
 */
public class StudentEvaluation {
    private int id;
    private int studentId;
    private double dsaScore;
    private double aiDependencyScore;
    private String topAlgorithms;
    private String topDataStructures;
    private int totalAnalyzed;
    private LocalDateTime evaluatedAt;

    // Convenience for display
    private String studentUsername;

    public StudentEvaluation() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public double getDsaScore() { return dsaScore; }
    public void setDsaScore(double dsaScore) { this.dsaScore = dsaScore; }

    public double getAiDependencyScore() { return aiDependencyScore; }
    public void setAiDependencyScore(double aiDependencyScore) { this.aiDependencyScore = aiDependencyScore; }

    public String getTopAlgorithms() { return topAlgorithms; }
    public void setTopAlgorithms(String topAlgorithms) { this.topAlgorithms = topAlgorithms; }

    public String getTopDataStructures() { return topDataStructures; }
    public void setTopDataStructures(String topDataStructures) { this.topDataStructures = topDataStructures; }

    public int getTotalAnalyzed() { return totalAnalyzed; }
    public void setTotalAnalyzed(int totalAnalyzed) { this.totalAnalyzed = totalAnalyzed; }

    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public String getStudentUsername() { return studentUsername; }
    public void setStudentUsername(String studentUsername) { this.studentUsername = studentUsername; }

    /** "Tốt", "Trung bình", "Yếu" – chỉ số tổng hợp dựa trên DSA và AI dep. */
    public String overallLabel() {
        if (totalAnalyzed == 0) return "Chưa có dữ liệu";
        double combined = dsaScore - 0.6 * aiDependencyScore;
        if (combined >= 5.0)   return "Tốt";
        if (combined >= 2.0)   return "Trung bình";
        return "Yếu / Nghi ngờ";
    }
}
