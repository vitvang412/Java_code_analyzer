package com.codeanalyzer.model;

import java.time.LocalDateTime;

public class AnalysisResult {
    private int id;
    private int submissionId;
    private String dataStructures;
    private String algorithms;
    private int aiUsageScore;          // 0-10
    private String aiUsageReason;
    private String complexityEstimate;
    private String summary;
    private LocalDateTime analyzedAt;

    // Convenience fields for display
    private String problemName;
    private String studentUsername;

    public AnalysisResult() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSubmissionId() { return submissionId; }
    public void setSubmissionId(int submissionId) { this.submissionId = submissionId; }

    public String getDataStructures() { return dataStructures; }
    public void setDataStructures(String dataStructures) { this.dataStructures = dataStructures; }

    public String getAlgorithms() { return algorithms; }
    public void setAlgorithms(String algorithms) { this.algorithms = algorithms; }

    public int getAiUsageScore() { return aiUsageScore; }
    public void setAiUsageScore(int aiUsageScore) { this.aiUsageScore = aiUsageScore; }

    public String getAiUsageReason() { return aiUsageReason; }
    public void setAiUsageReason(String aiUsageReason) { this.aiUsageReason = aiUsageReason; }

    public String getComplexityEstimate() { return complexityEstimate; }
    public void setComplexityEstimate(String complexityEstimate) { this.complexityEstimate = complexityEstimate; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public String getProblemName() { return problemName; }
    public void setProblemName(String problemName) { this.problemName = problemName; }

    public String getStudentUsername() { return studentUsername; }
    public void setStudentUsername(String studentUsername) { this.studentUsername = studentUsername; }
}
