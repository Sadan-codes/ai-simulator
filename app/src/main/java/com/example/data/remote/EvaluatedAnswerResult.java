package com.example.data.remote;

import java.util.List;

public class EvaluatedAnswerResult {
    private int overallScore;
    private int technicalAccuracy;
    private int communication;
    private int problemSolving;
    private int confidence;
    private String feedback;
    private List<String> strengths;
    private List<String> weaknesses;
    private String idealAnswer;

    public EvaluatedAnswerResult(
            int overallScore,
            int technicalAccuracy,
            int communication,
            int problemSolving,
            int confidence,
            String feedback,
            List<String> strengths,
            List<String> weaknesses,
            String idealAnswer
    ) {
        this.overallScore = overallScore;
        this.technicalAccuracy = technicalAccuracy;
        this.communication = communication;
        this.problemSolving = problemSolving;
        this.confidence = confidence;
        this.feedback = feedback;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.idealAnswer = idealAnswer;
    }

    public int getOverallScore() { return overallScore; }
    public int getTechnicalAccuracy() { return technicalAccuracy; }
    public int getCommunication() { return communication; }
    public int getProblemSolving() { return problemSolving; }
    public int getConfidence() { return confidence; }
    public String getFeedback() { return feedback; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getWeaknesses() { return weaknesses; }
    public String getIdealAnswer() { return idealAnswer; }
}
