package com.codeanalyzer.model;

import java.time.LocalDateTime;

public class Submission {
    private int id;
    private int studentId;
    private String problemId;
    private String problemName;
    private String language;
    private String sourceCode;
    private String verdict;
    private LocalDateTime submittedAt;
    private LocalDateTime crawledAt;

    // For display convenience
    private String studentUsername;

    public Submission() {}

    public Submission(int studentId, String problemId, String problemName,
                      String language, String sourceCode, String verdict,
                      LocalDateTime submittedAt) {
        this.studentId = studentId;
        this.problemId = problemId;
        this.problemName = problemName;
        this.language = language;
        this.sourceCode = sourceCode;
        this.verdict = verdict;
        this.submittedAt = submittedAt;
        this.crawledAt = LocalDateTime.now();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getProblemId() { return problemId; }
    public void setProblemId(String problemId) { this.problemId = problemId; }

    public String getProblemName() { return problemName; }
    public void setProblemName(String problemName) { this.problemName = problemName; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getCrawledAt() { return crawledAt; }
    public void setCrawledAt(LocalDateTime crawledAt) { this.crawledAt = crawledAt; }

    public String getStudentUsername() { return studentUsername; }
    public void setStudentUsername(String studentUsername) { this.studentUsername = studentUsername; }

    @Override
    public String toString() {
        return "[" + problemId + "] " + problemName + " - " + verdict;
    }
}
