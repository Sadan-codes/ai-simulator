package com.example.data.remote;

import java.util.List;

public class GeneratedReportResult {
    private int overallScore;
    private int technicalScore;
    private int communicationScore;
    private int problemSolvingScore;
    private int confidenceScore;
    private String verdict;
    private String summaryFeedback;
    private List<String> learningRoadmap;

    public GeneratedReportResult(
            int overallScore,
            int technicalScore,
            int communicationScore,
            int problemSolvingScore,
            int confidenceScore,
            String verdict,
            String summaryFeedback,
            List<String> learningRoadmap
    ) {
        this.overallScore = overallScore;
        this.technicalScore = technicalScore;
        this.communicationScore = communicationScore;
        this.problemSolvingScore = problemSolvingScore;
        this.confidenceScore = confidenceScore;
        this.verdict = verdict;
        this.summaryFeedback = summaryFeedback;
        this.learningRoadmap = learningRoadmap;
    }

    public int getOverallScore() { return overallScore; }
    public int getTechnicalScore() { return technicalScore; }
    public int getCommunicationScore() { return communicationScore; }
    public int getProblemSolvingScore() { return problemSolvingScore; }
    public int getConfidenceScore() { return confidenceScore; }
    public String getVerdict() { return verdict; }
    public String getSummaryFeedback() { return summaryFeedback; }
    public List<String> getLearningRoadmap() { return learningRoadmap; }
}
