package com.codeanalyzer.database;

import com.codeanalyzer.model.StudentEvaluation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng student_evaluations.
 * Mỗi student chỉ giữ 1 row mới nhất: dùng ON DUPLICATE KEY UPDATE
 * dựa theo UNIQUE index trên student_id (được bổ sung runtime nếu chưa có).
 */
public class StudentEvaluationDAO {

    private Connection conn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    /** Ghi đè đánh giá cho sinh viên: xoá bản ghi cũ rồi insert bản mới. */
    public int save(StudentEvaluation e) {
        // Xoá các bản cũ của student này để mỗi student chỉ còn 1 bản đánh giá mới nhất
        String delSql = "DELETE FROM student_evaluations WHERE student_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(delSql)) {
            ps.setInt(1, e.getStudentId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[StudentEvaluationDAO] delete old error: " + ex.getMessage());
        }

        String sql = """
            INSERT INTO student_evaluations
                (student_id, dsa_score, ai_dependency_score, top_algorithms,
                 top_data_structures, total_analyzed, evaluated_at)
            VALUES (?, ?, ?, ?, ?, ?, NOW())
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getStudentId());
            ps.setDouble(2, e.getDsaScore());
            ps.setDouble(3, e.getAiDependencyScore());
            ps.setString(4, e.getTopAlgorithms());
            ps.setString(5, e.getTopDataStructures());
            ps.setInt(6, e.getTotalAnalyzed());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            System.err.println("[StudentEvaluationDAO] save error: " + ex.getMessage());
        }
        return -1;
    }

    public StudentEvaluation findByStudentId(int studentId) {
        String sql = """
            SELECT se.*, st.username AS student_username
            FROM student_evaluations se
            JOIN students st ON st.id = se.student_id
            WHERE se.student_id = ?
            ORDER BY se.evaluated_at DESC
            LIMIT 1
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException ex) {
            System.err.println("[StudentEvaluationDAO] findByStudentId error: " + ex.getMessage());
        }
        return null;
    }

    public List<StudentEvaluation> findAll() {
        List<StudentEvaluation> list = new ArrayList<>();
        String sql = """
            SELECT se.*, st.username AS student_username
            FROM student_evaluations se
            JOIN students st ON st.id = se.student_id
            ORDER BY se.evaluated_at DESC
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException ex) {
            System.err.println("[StudentEvaluationDAO] findAll error: " + ex.getMessage());
        }
        return list;
    }

    private StudentEvaluation map(ResultSet rs) throws SQLException {
        StudentEvaluation e = new StudentEvaluation();
        e.setId(rs.getInt("id"));
        e.setStudentId(rs.getInt("student_id"));
        e.setDsaScore(rs.getDouble("dsa_score"));
        e.setAiDependencyScore(rs.getDouble("ai_dependency_score"));
        e.setTopAlgorithms(rs.getString("top_algorithms"));
        e.setTopDataStructures(rs.getString("top_data_structures"));
        e.setTotalAnalyzed(rs.getInt("total_analyzed"));
        Timestamp ts = rs.getTimestamp("evaluated_at");
        if (ts != null) e.setEvaluatedAt(ts.toLocalDateTime());
        try { e.setStudentUsername(rs.getString("student_username")); } catch (SQLException ignored) {}
        return e;
    }
}
