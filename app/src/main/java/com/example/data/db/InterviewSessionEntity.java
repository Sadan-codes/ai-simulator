package com.example.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "interview_sessions")
public class InterviewSessionEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String userId = "default_user";
    private String role;
    private String difficulty;
    private String experienceLevel;
    private String companyPreset = "General Tech";
    private int questionCount = 5;
    private int overallScore = 0;
    private String verdict = "Pending";
    private String summaryFeedback = "";
    private int fillerWordTotal = 0;
    private int confidenceAvgScore = 0;
    private int technicalScore = 0;
    private int communicationScore = 0;
    private int problemSolvingScore = 0;
    private String roadmapJson = "[]";
    private boolean isCompleted = false;
    private long createdAt = System.currentTimeMillis();

    public InterviewSessionEntity() {}

    public InterviewSessionEntity(
            long id,
            String userId,
            String role,
            String difficulty,
            String experienceLevel,
            String companyPreset,
            int questionCount,
            int overallScore,
            String verdict,
            String summaryFeedback,
            int fillerWordTotal,
            int confidenceAvgScore,
            int technicalScore,
            int communicationScore,
            int problemSolvingScore,
            String roadmapJson,
            boolean isCompleted,
            long createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.role = role;
        this.difficulty = difficulty;
        this.experienceLevel = experienceLevel;
        this.companyPreset = companyPreset;
        this.questionCount = questionCount;
        this.overallScore = overallScore;
        this.verdict = verdict;
        this.summaryFeedback = summaryFeedback;
        this.fillerWordTotal = fillerWordTotal;
        this.confidenceAvgScore = confidenceAvgScore;
        this.technicalScore = technicalScore;
        this.communicationScore = communicationScore;
        this.problemSolvingScore = problemSolvingScore;
        this.roadmapJson = roadmapJson;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }

    public String getCompanyPreset() { return companyPreset; }
    public void setCompanyPreset(String companyPreset) { this.companyPreset = companyPreset; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public String getSummaryFeedback() { return summaryFeedback; }
    public void setSummaryFeedback(String summaryFeedback) { this.summaryFeedback = summaryFeedback; }

    public int getFillerWordTotal() { return fillerWordTotal; }
    public void setFillerWordTotal(int fillerWordTotal) { this.fillerWordTotal = fillerWordTotal; }

    public int getConfidenceAvgScore() { return confidenceAvgScore; }
    public void setConfidenceAvgScore(int confidenceAvgScore) { this.confidenceAvgScore = confidenceAvgScore; }

    public int getTechnicalScore() { return technicalScore; }
    public void setTechnicalScore(int technicalScore) { this.technicalScore = technicalScore; }

    public int getCommunicationScore() { return communicationScore; }
    public void setCommunicationScore(int communicationScore) { this.communicationScore = communicationScore; }

    public int getProblemSolvingScore() { return problemSolvingScore; }
    public void setProblemSolvingScore(int problemSolvingScore) { this.problemSolvingScore = problemSolvingScore; }

    public String getRoadmapJson() { return roadmapJson; }
    public void setRoadmapJson(String roadmapJson) { this.roadmapJson = roadmapJson; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
