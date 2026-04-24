package com.codeanalyzer.database;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Reads sql/schema.sql and creates all tables if they don't exist.
 * Called once at startup from Main.java.
 */
public class DatabaseInitializer {

    public static void initialize() {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        if (conn == null) {
            System.err.println("[DB] Cannot initialize – no DB connection.");
            return;
        }

        String[] statements = buildStatements();
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                if (!sql.isBlank()) {
                    stmt.execute(sql.trim());
                }
            }
            System.out.println("[DB] Tables initialized successfully.");
        } catch (Exception e) {
            System.err.println("[DB] Initialization error: " + e.getMessage());
        }
    }

    private static String[] buildStatements() {
        // Inline DDL so the app works without reading the .sql file at runtime
        return new String[]{
            // students
            """
            CREATE TABLE IF NOT EXISTS students (
                id              INT AUTO_INCREMENT PRIMARY KEY,
                username        VARCHAR(100) NOT NULL,
                platform        ENUM('CODEFORCES','VJUDGE') NOT NULL DEFAULT 'CODEFORCES',
                display_name    VARCHAR(200),
                added_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                last_crawled_at DATETIME,
                is_active       TINYINT(1) NOT NULL DEFAULT 1,
                UNIQUE KEY uk_username_platform (username, platform)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // submissions
            """
            CREATE TABLE IF NOT EXISTS submissions (
                id           INT AUTO_INCREMENT PRIMARY KEY,
                student_id   INT NOT NULL,
                problem_id   VARCHAR(100),
                problem_name VARCHAR(500),
                language     VARCHAR(100),
                source_code  LONGTEXT,
                verdict      VARCHAR(100),
                submitted_at DATETIME,
                crawled_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
                INDEX idx_student (student_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // analysis_results
            """
            CREATE TABLE IF NOT EXISTS analysis_results (
                id                  INT AUTO_INCREMENT PRIMARY KEY,
                submission_id       INT NOT NULL UNIQUE,
                data_structures     TEXT,
                algorithms          TEXT,
                ai_usage_score      INT DEFAULT 0,
                ai_usage_reason     TEXT,
                complexity_estimate VARCHAR(100),
                summary             TEXT,
                analyzed_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """,

            // student_evaluations
            """
            CREATE TABLE IF NOT EXISTS student_evaluations (
                id                  INT AUTO_INCREMENT PRIMARY KEY,
                student_id          INT NOT NULL,
                dsa_score           DECIMAL(5,2) DEFAULT 0,
                ai_dependency_score DECIMAL(5,2) DEFAULT 0,
                top_algorithms      TEXT,
                top_data_structures TEXT,
                total_analyzed      INT DEFAULT 0,
                evaluated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
                INDEX idx_student_eval (student_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """
        };
    }
}
