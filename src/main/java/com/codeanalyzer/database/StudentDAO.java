package com.codeanalyzer.database;

import com.codeanalyzer.model.Student;
import com.codeanalyzer.model.PlatformType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    private Connection conn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    /** Insert a student. Returns generated id, or -1 on error. */
    public int save(Student s) {
        String sql = """
            INSERT INTO students (username, platform, display_name, added_at, is_active)
            VALUES (?, ?, ?, NOW(), 1)
            ON DUPLICATE KEY UPDATE is_active = 1
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getUsername());
            ps.setString(2, s.getPlatform().name());
            ps.setString(3, s.getDisplayName());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            // ON DUPLICATE KEY UPDATE -> no new key; fetch existing id
            return findByUsernameAndPlatform(s.getUsername(), s.getPlatform())
                    .stream().findFirst().map(Student::getId).orElse(-1);
        } catch (SQLException e) {
            System.err.println("[StudentDAO] save error: " + e.getMessage());
            return -1;
        }
    }

    public List<Student> findAll() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE is_active = 1 ORDER BY added_at DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("[StudentDAO] findAll error: " + e.getMessage());
        }
        return list;
    }

    public List<Student> findByUsernameAndPlatform(String username, PlatformType platform) {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE username=? AND platform=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, platform.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO] findByUsernameAndPlatform error: " + e.getMessage());
        }
        return list;
    }

    public Student findById(int id) {
        String sql = "SELECT * FROM students WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("[StudentDAO] findById error: " + e.getMessage());
        }
        return null;
    }

    public void updateLastCrawled(int studentId, LocalDateTime time) {
        String sql = "UPDATE students SET last_crawled_at=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(time));
            ps.setInt(2, studentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[StudentDAO] updateLastCrawled error: " + e.getMessage());
        }
    }

    public void deactivate(int studentId) {
        String sql = "UPDATE students SET is_active=0 WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[StudentDAO] deactivate error: " + e.getMessage());
        }
    }

    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setUsername(rs.getString("username"));
        s.setPlatform(PlatformType.valueOf(rs.getString("platform")));
        s.setDisplayName(rs.getString("display_name"));
        Timestamp addedAt = rs.getTimestamp("added_at");
        if (addedAt != null) s.setAddedAt(addedAt.toLocalDateTime());
        Timestamp lastCrawled = rs.getTimestamp("last_crawled_at");
        if (lastCrawled != null) s.setLastCrawledAt(lastCrawled.toLocalDateTime());
        s.setActive(rs.getInt("is_active") == 1);
        return s;
    }
}
