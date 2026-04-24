package com.codeanalyzer.database;

import com.codeanalyzer.model.AnalysisResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnalysisResultDAO {

    private Connection conn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    public int save(AnalysisResult r) {
        String sql = """
            INSERT INTO analysis_results
              (submission_id, data_structures, algorithms, ai_usage_score,
               ai_usage_reason, complexity_estimate, summary, analyzed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
              data_structures=VALUES(data_structures),
              algorithms=VALUES(algorithms),
              ai_usage_score=VALUES(ai_usage_score),
              ai_usage_reason=VALUES(ai_usage_reason),
              complexity_estimate=VALUES(complexity_estimate),
              summary=VALUES(summary),
              analyzed_at=NOW()
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getSubmissionId());
            ps.setString(2, r.getDataStructures());
            ps.setString(3, r.getAlgorithms());
            ps.setInt(4, r.getAiUsageScore());
            ps.setString(5, r.getAiUsageReason());
            ps.setString(6, r.getComplexityEstimate());
            ps.setString(7, r.getSummary());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[AnalysisResultDAO] save error: " + e.getMessage());
        }
        return -1;
    }

    public AnalysisResult findBySubmissionId(int submissionId) {
        String sql = """
            SELECT ar.*, s.problem_name, st.username as student_username
            FROM analysis_results ar
            JOIN submissions s ON s.id = ar.submission_id
            JOIN students st ON st.id = s.student_id
            WHERE ar.submission_id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("[AnalysisResultDAO] findBySubmission error: " + e.getMessage());
        }
        return null;
    }

    public List<AnalysisResult> findByStudentId(int studentId) {
        List<AnalysisResult> list = new ArrayList<>();
        String sql = """
            SELECT ar.*, s.problem_name, st.username as student_username
            FROM analysis_results ar
            JOIN submissions s ON s.id = ar.submission_id
            JOIN students st ON st.id = s.student_id
            WHERE s.student_id = ?
            ORDER BY ar.analyzed_at DESC
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AnalysisResultDAO] findByStudentId error: " + e.getMessage());
        }
        return list;
    }

    public List<AnalysisResult> findAll(int limit) {
        List<AnalysisResult> list = new ArrayList<>();
        String sql = """
            SELECT ar.*, s.problem_name, st.username as student_username
            FROM analysis_results ar
            JOIN submissions s ON s.id = ar.submission_id
            JOIN students st ON st.id = s.student_id
            ORDER BY ar.analyzed_at DESC
            LIMIT ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AnalysisResultDAO] findAll error: " + e.getMessage());
        }
        return list;
    }

    private AnalysisResult map(ResultSet rs) throws SQLException {
        AnalysisResult r = new AnalysisResult();
        r.setId(rs.getInt("id"));
        r.setSubmissionId(rs.getInt("submission_id"));
        r.setDataStructures(rs.getString("data_structures"));
        r.setAlgorithms(rs.getString("algorithms"));
        r.setAiUsageScore(rs.getInt("ai_usage_score"));
        r.setAiUsageReason(rs.getString("ai_usage_reason"));
        r.setComplexityEstimate(rs.getString("complexity_estimate"));
        r.setSummary(rs.getString("summary"));
        Timestamp ts = rs.getTimestamp("analyzed_at");
        if (ts != null) r.setAnalyzedAt(ts.toLocalDateTime());
        try { r.setProblemName(rs.getString("problem_name")); } catch (SQLException ignored) {}
        try { r.setStudentUsername(rs.getString("student_username")); } catch (SQLException ignored) {}
        return r;
    }
}
