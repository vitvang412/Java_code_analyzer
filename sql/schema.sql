-- Code Analyzer System - Database Schema
-- Run this script once to initialize the database

CREATE DATABASE IF NOT EXISTS code_analyzer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE code_analyzer;

-- ============================================================
-- TABLE: students
-- ============================================================
CREATE TABLE IF NOT EXISTS students (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL,
    platform      ENUM('CODEFORCES', 'VJUDGE') NOT NULL DEFAULT 'CODEFORCES',
    display_name  VARCHAR(200),
    added_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_crawled_at DATETIME,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    UNIQUE KEY uk_username_platform (username, platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- TABLE: submissions
-- ============================================================
CREATE TABLE IF NOT EXISTS submissions (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    student_id    INT          NOT NULL,
    problem_id    VARCHAR(100),
    problem_name  VARCHAR(500),
    language      VARCHAR(100),
    source_code   LONGTEXT,
    verdict       VARCHAR(100),
    submitted_at  DATETIME,
    crawled_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_student (student_id),
    INDEX idx_submitted_at (submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- TABLE: analysis_results
-- ============================================================
CREATE TABLE IF NOT EXISTS analysis_results (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    submission_id        INT          NOT NULL UNIQUE,
    data_structures      TEXT,
    algorithms           TEXT,
    ai_usage_score       INT          DEFAULT 0,
    ai_usage_reason      TEXT,
    complexity_estimate  VARCHAR(100),
    summary              TEXT,
    analyzed_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- TABLE: student_evaluations
-- ============================================================
CREATE TABLE IF NOT EXISTS student_evaluations (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    student_id           INT          NOT NULL,
    dsa_score            DECIMAL(5,2) DEFAULT 0,
    ai_dependency_score  DECIMAL(5,2) DEFAULT 0,
    top_algorithms       TEXT,
    top_data_structures  TEXT,
    total_analyzed       INT          DEFAULT 0,
    evaluated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_student_eval (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
