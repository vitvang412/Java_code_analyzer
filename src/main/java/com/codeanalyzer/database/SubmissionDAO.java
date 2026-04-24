package com.codeanalyzer.database;

import com.codeanalyzer.model.Submission;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubmissionDAO {

    private Connection conn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Saves a submission. Skips duplicates (same student + problem + submitted_at).
     * Returns generated id, or -1 if skipped/error.
     */
    public int save(Submission sub) {
        String sql = """
            INSERT IGNORE INTO submissions
              (student_id, problem_id, problem_name, language, source_code, verdict, submitted_at, crawled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sub.getStudentId());
            ps.setString(2, sub.getProblemId());
            ps.setString(3, sub.getProblemName());
            ps.setString(4, sub.getLanguage());
            ps.setString(5, sub.getSourceCode());
            ps.setString(6, sub.getVerdict());
            if (sub.getSubmittedAt() != null)
                ps.setTimestamp(7, Timestamp.valueOf(sub.getSubmittedAt()));
            else
                ps.setNull(7, Types.TIMESTAMP);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] save error: " + e.getMessage());
        }
        return -1;
    }

    public List<Submission> findByStudentId(int studentId) {
        List<Submission> list = new ArrayList<>();
        String sql = """
            SELECT s.*, st.username as student_username
            FROM submissions s
            JOIN students st ON st.id = s.student_id
            WHERE s.student_id = ?
            ORDER BY s.submitted_at DESC
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] findByStudentId error: " + e.getMessage());
        }
        return list;
    }

    public List<Submission> findAll(int limit) {
        List<Submission> list = new ArrayList<>();
        String sql = """
            SELECT s.*, st.username as student_username
            FROM submissions s
            JOIN students st ON st.id = s.student_id
            ORDER BY s.crawled_at DESC
            LIMIT ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] findAll error: " + e.getMessage());
        }
        return list;
    }

    /** Return submissions that do NOT yet have an analysis_result */
    public List<Submission> findUnanalyzed(int limit) {
        List<Submission> list = new ArrayList<>();
        String sql = """
            SELECT s.*, st.username as student_username
            FROM submissions s
            JOIN students st ON st.id = s.student_id
            LEFT JOIN analysis_results ar ON ar.submission_id = s.id
            WHERE ar.id IS NULL AND s.source_code IS NOT NULL AND s.source_code != ''
            ORDER BY s.crawled_at ASC
            LIMIT ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] findUnanalyzed error: " + e.getMessage());
        }
        return list;
    }

    /** Return unanalyzed submissions for a specific student */
    public List<Submission> findUnanalyzedByStudent(int studentId, int limit) {
        List<Submission> list = new ArrayList<>();
        String sql = """
            SELECT s.*, st.username as student_username
            FROM submissions s
            JOIN students st ON st.id = s.student_id
            LEFT JOIN analysis_results ar ON ar.submission_id = s.id
            WHERE ar.id IS NULL AND s.source_code IS NOT NULL AND s.source_code != ''
              AND s.student_id = ?
            ORDER BY s.crawled_at ASC
            LIMIT ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] findUnanalyzedByStudent error: " + e.getMessage());
        }
        return list;
    }

    public int countByStudentId(int studentId) {
        String sql = "SELECT COUNT(*) FROM submissions WHERE student_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[SubmissionDAO] count error: " + e.getMessage());
        }
        return 0;
    }

    private Submission map(ResultSet rs) throws SQLException {
        Submission s = new Submission();
        s.setId(rs.getInt("id"));
        s.setStudentId(rs.getInt("student_id"));
        s.setProblemId(rs.getString("problem_id"));
        s.setProblemName(rs.getString("problem_name"));
        s.setLanguage(rs.getString("language"));
        s.setSourceCode(rs.getString("source_code"));
        s.setVerdict(rs.getString("verdict"));
        Timestamp submitted = rs.getTimestamp("submitted_at");
        if (submitted != null) s.setSubmittedAt(submitted.toLocalDateTime());
        Timestamp crawled = rs.getTimestamp("crawled_at");
        if (crawled != null) s.setCrawledAt(crawled.toLocalDateTime());
        try { s.setStudentUsername(rs.getString("student_username")); } catch (SQLException ignored) {}
        return s;
    }
}
