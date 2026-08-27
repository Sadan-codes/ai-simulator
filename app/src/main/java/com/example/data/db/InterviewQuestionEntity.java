package com.example.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "interview_questions")
public class InterviewQuestionEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long sessionId;
    private int questionIndex;
    private String questionText;
    private String category = "Technical";
    private String userAnswer = "";
    private String feedback = "";
    private int score = 0;
    private int technicalAccuracyScore = 0;
    private int communicationScore = 0;
    private int problemSolvingScore = 0;
    private int confidenceScore = 0;
    private String strengthsJson = "[]";
    private String weaknessesJson = "[]";
    private String idealAnswer = "";
    private int fillerWordCount = 0;
    private int durationSeconds = 0;
    private boolean isEvaluated = false;

    public InterviewQuestionEntity() {}

    public InterviewQuestionEntity(
            long id,
            long sessionId,
            int questionIndex,
            String questionText,
            String category,
            String userAnswer,
            String feedback,
            int score,
            int technicalAccuracyScore,
            int communicationScore,
            int problemSolvingScore,
            int confidenceScore,
            String strengthsJson,
            String weaknessesJson,
            String idealAnswer,
            int fillerWordCount,
            int durationSeconds,
            boolean isEvaluated
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.questionIndex = questionIndex;
        this.questionText = questionText;
        this.category = category;
        this.userAnswer = userAnswer;
        this.feedback = feedback;
        this.score = score;
        this.technicalAccuracyScore = technicalAccuracyScore;
        this.communicationScore = communicationScore;
        this.problemSolvingScore = problemSolvingScore;
        this.confidenceScore = confidenceScore;
        this.strengthsJson = strengthsJson;
        this.weaknessesJson = weaknessesJson;
        this.idealAnswer = idealAnswer;
        this.fillerWordCount = fillerWordCount;
        this.durationSeconds = durationSeconds;
        this.isEvaluated = isEvaluated;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSessionId() { return sessionId; }
    public void setSessionId(long sessionId) { this.sessionId = sessionId; }

    public int getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(int questionIndex) { this.questionIndex = questionIndex; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getTechnicalAccuracyScore() { return technicalAccuracyScore; }
    public void setTechnicalAccuracyScore(int technicalAccuracyScore) { this.technicalAccuracyScore = technicalAccuracyScore; }

    public int getCommunicationScore() { return communicationScore; }
    public void setCommunicationScore(int communicationScore) { this.communicationScore = communicationScore; }

    public int getProblemSolvingScore() { return problemSolvingScore; }
    public void setProblemSolvingScore(int problemSolvingScore) { this.problemSolvingScore = problemSolvingScore; }

    public int getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getStrengthsJson() { return strengthsJson; }
    public void setStrengthsJson(String strengthsJson) { this.strengthsJson = strengthsJson; }

    public String getWeaknessesJson() { return weaknessesJson; }
    public void setWeaknessesJson(String weaknessesJson) { this.weaknessesJson = weaknessesJson; }

    public String getIdealAnswer() { return idealAnswer; }
    public void setIdealAnswer(String idealAnswer) { this.idealAnswer = idealAnswer; }

    public int getFillerWordCount() { return fillerWordCount; }
    public void setFillerWordCount(int fillerWordCount) { this.fillerWordCount = fillerWordCount; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public boolean isEvaluated() { return isEvaluated; }
    public void setEvaluated(boolean evaluated) { isEvaluated = evaluated; }
}
