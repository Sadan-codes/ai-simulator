package com.example.data;

import com.example.data.db.InterviewDao;
import com.example.data.db.InterviewQuestionEntity;
import com.example.data.db.InterviewSessionEntity;
import com.example.data.db.ResumeProfileEntity;
import com.example.data.db.UserEntity;
import com.example.data.remote.EvaluatedAnswerResult;
import com.example.data.remote.GeminiService;
import com.example.data.remote.GeneratedReportResult;
import com.example.data.remote.ParsedResumeResult;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import kotlin.Pair;
import kotlinx.coroutines.flow.Flow;

/**
 * Pure Java Repository managing local Room SQLite data operations,
 * background threading with ExecutorService, and Gemini AI interview intelligence.
 */
public class InterviewRepository {

    private final InterviewDao dao;
    private final GeminiService geminiService;
    private final ExecutorService executorService;

    public InterviewRepository(InterviewDao dao) {
        this(dao, new GeminiService());
    }

    public InterviewRepository(InterviewDao dao, GeminiService geminiService) {
        this.dao = dao;
        this.geminiService = geminiService;
        this.executorService = Executors.newFixedThreadPool(4);
    }

    // ==================== User Operations ====================

    public Flow<UserEntity> getCurrentUser() {
        return dao.getUser("default_user");
    }

    public void saveUser(UserEntity user) {
        executorService.execute(() -> dao.insertUser(user));
    }

    // ==================== Resume Operations ====================

    public Flow<List<ResumeProfileEntity>> getAllResumes() {
        return dao.getAllResumes();
    }

    public ResumeProfileEntity getResumeById(long id) {
        try {
            return executorService.submit(() -> dao.getResumeById(id)).get();
        } catch (Exception e) {
            return null;
        }
    }

    public long parseAndSaveResume(String rawText, String title) {
        try {
            return executorService.submit(() -> {
                ParsedResumeResult parsed = geminiService.parseResumeText(rawText);

                String displayTitle = (parsed.getCandidateName() != null && !parsed.getCandidateName().isEmpty())
                        ? parsed.getCandidateName() + "'s Resume"
                        : title;

                ResumeProfileEntity entity = new ResumeProfileEntity(
                        0,
                        "default_user",
                        displayTitle,
                        rawText,
                        parsed.getCandidateName(),
                        new JSONArray(parsed.getSkills()).toString(),
                        new JSONArray(parsed.getLanguages()).toString(),
                        new JSONArray(parsed.getFrameworks()).toString(),
                        new JSONArray(parsed.getProjects()).toString(),
                        parsed.getEducation(),
                        parsed.getYearsExperience(),
                        System.currentTimeMillis()
                );

                long resumeId = dao.insertResume(entity);

                String candidateName = (parsed.getCandidateName() != null && !parsed.getCandidateName().isEmpty())
                        ? parsed.getCandidateName()
                        : "Candidate";

                UserEntity user = new UserEntity(
                        "default_user",
                        candidateName,
                        "alex@example.com",
                        "Software Engineer",
                        resumeId,
                        System.currentTimeMillis()
                );
                dao.insertUser(user);

                return resumeId;
            }).get();
        } catch (Exception e) {
            return 0L;
        }
    }

    public long parseAndSaveResumeImage(String base64Jpeg, String title) {
        try {
            return executorService.submit(() -> {
                ParsedResumeResult parsed = geminiService.parseResumeImage(base64Jpeg);

                String displayTitle = (parsed.getCandidateName() != null && !parsed.getCandidateName().isEmpty())
                        ? parsed.getCandidateName() + "'s Resume Photo"
                        : title;

                String rawText = "Photo of Resume parsed via Gemini Vision AI.\n"
                        + "Skills: " + String.join(", ", parsed.getSkills()) + "\n"
                        + "Languages: " + String.join(", ", parsed.getLanguages()) + "\n"
                        + "Frameworks: " + String.join(", ", parsed.getFrameworks()) + "\n"
                        + "Education: " + parsed.getEducation();

                ResumeProfileEntity entity = new ResumeProfileEntity(
                        0,
                        "default_user",
                        displayTitle,
                        rawText,
                        parsed.getCandidateName(),
                        new JSONArray(parsed.getSkills()).toString(),
                        new JSONArray(parsed.getLanguages()).toString(),
                        new JSONArray(parsed.getFrameworks()).toString(),
                        new JSONArray(parsed.getProjects()).toString(),
                        parsed.getEducation(),
                        parsed.getYearsExperience(),
                        System.currentTimeMillis()
                );

                long resumeId = dao.insertResume(entity);

                String candidateName = (parsed.getCandidateName() != null && !parsed.getCandidateName().isEmpty())
                        ? parsed.getCandidateName()
                        : "Candidate";

                UserEntity user = new UserEntity(
                        "default_user",
                        candidateName,
                        "alex@example.com",
                        "Software Engineer",
                        resumeId,
                        System.currentTimeMillis()
                );
                dao.insertUser(user);

                return resumeId;
            }).get();
        } catch (Exception e) {
            return 0L;
        }
    }

    public void deleteResume(long id) {
        executorService.execute(() -> dao.deleteResumeById(id));
    }

    // ==================== Interview Session Operations ====================

    public Flow<List<InterviewSessionEntity>> getAllSessions() {
        return dao.getAllSessions();
    }

    public Flow<InterviewSessionEntity> getSessionById(long sessionId) {
        return dao.getSessionById(sessionId);
    }

    public Flow<List<InterviewQuestionEntity>> getQuestionsForSession(long sessionId) {
        return dao.getQuestionsForSession(sessionId);
    }

    public long createInterviewSession(
            String role,
            String difficulty,
            String experienceLevel,
            String companyPreset,
            int questionCount,
            List<String> resumeSkills
    ) {
        try {
            return executorService.submit(() -> {
                InterviewSessionEntity sessionEntity = new InterviewSessionEntity(
                        0,
                        "default_user",
                        role,
                        difficulty,
                        experienceLevel,
                        companyPreset,
                        questionCount,
                        0,
                        "Pending",
                        "",
                        0,
                        0,
                        0,
                        0,
                        0,
                        "[]",
                        false,
                        System.currentTimeMillis()
                );

                long sessionId = dao.insertSession(sessionEntity);

                Pair<String, String> qResult = geminiService.generateQuestion(
                        role,
                        difficulty,
                        experienceLevel,
                        companyPreset,
                        1,
                        questionCount,
                        resumeSkills
                );

                InterviewQuestionEntity firstQuestion = new InterviewQuestionEntity(
                        0,
                        sessionId,
                        1,
                        qResult.getFirst(),
                        qResult.getSecond(),
                        "",
                        "",
                        0,
                        0,
                        0,
                        0,
                        0,
                        "[]",
                        "[]",
                        "",
                        0,
                        0,
                        false
                );

                dao.insertQuestion(firstQuestion);
                return sessionId;
            }).get();
        } catch (Exception e) {
            return 0L;
        }
    }

    public Pair<InterviewQuestionEntity, String> submitAndEvaluateAnswer(
            long sessionId,
            int questionIndex,
            long questionId,
            String userAnswer,
            int fillerWordCount,
            int durationSeconds,
            List<String> resumeSkills
    ) {
        try {
            return executorService.submit(() -> {
                InterviewSessionEntity session = dao.getSessionSync(sessionId);
                if (session == null) {
                    InterviewQuestionEntity fallback = new InterviewQuestionEntity(
                            0, sessionId, questionIndex, "", "Technical", "", "", 0, 0, 0, 0, 0, "[]", "[]", "", 0, 0, false
                    );
                    return new Pair<>(fallback, (String) null);
                }

                List<InterviewQuestionEntity> existingQuestions = dao.getQuestionsForSessionSync(sessionId);
                InterviewQuestionEntity currentQuestionObj = null;
                for (InterviewQuestionEntity q : existingQuestions) {
                    if (q.getId() == questionId || q.getQuestionIndex() == questionIndex) {
                        currentQuestionObj = q;
                        break;
                    }
                }

                String questionText = currentQuestionObj != null ? currentQuestionObj.getQuestionText() : "";

                EvaluatedAnswerResult evaluation = geminiService.evaluateAnswer(
                        session.getRole(),
                        questionText,
                        userAnswer,
                        resumeSkills,
                        fillerWordCount
                );

                long targetQId = currentQuestionObj != null ? currentQuestionObj.getId() : 0L;
                String category = currentQuestionObj != null ? currentQuestionObj.getCategory() : "Technical";

                InterviewQuestionEntity updatedQuestion = new InterviewQuestionEntity(
                        targetQId,
                        sessionId,
                        questionIndex,
                        questionText,
                        category,
                        userAnswer,
                        evaluation.getFeedback(),
                        evaluation.getOverallScore(),
                        evaluation.getTechnicalAccuracy(),
                        evaluation.getCommunication(),
                        evaluation.getProblemSolving(),
                        evaluation.getConfidence(),
                        new JSONArray(evaluation.getStrengths()).toString(),
                        new JSONArray(evaluation.getWeaknesses()).toString(),
                        evaluation.getIdealAnswer(),
                        fillerWordCount,
                        durationSeconds,
                        true
                );

                dao.updateQuestion(updatedQuestion);

                String nextQuestionText = null;
                if (questionIndex < session.getQuestionCount()) {
                    int nextIndex = questionIndex + 1;
                    Pair<String, String> nextQResult = geminiService.generateQuestion(
                            session.getRole(),
                            session.getDifficulty(),
                            session.getExperienceLevel(),
                            session.getCompanyPreset(),
                            nextIndex,
                            session.getQuestionCount(),
                            resumeSkills,
                            questionText,
                            userAnswer
                    );

                    InterviewQuestionEntity nextQuestionObj = new InterviewQuestionEntity(
                            0,
                            sessionId,
                            nextIndex,
                            nextQResult.getFirst(),
                            nextQResult.getSecond(),
                            "",
                            "",
                            0,
                            0,
                            0,
                            0,
                            0,
                            "[]",
                            "[]",
                            "",
                            0,
                            0,
                            false
                    );

                    dao.insertQuestion(nextQuestionObj);
                    nextQuestionText = nextQResult.getFirst();
                } else {
                    finalizeSessionReportSync(sessionId);
                }

                return new Pair<>(updatedQuestion, nextQuestionText);
            }).get();
        } catch (Exception e) {
            InterviewQuestionEntity fallback = new InterviewQuestionEntity(
                    0, sessionId, questionIndex, "", "Technical", "", "", 0, 0, 0, 0, 0, "[]", "[]", "", 0, 0, false
            );
            return new Pair<>(fallback, (String) null);
        }
    }

    private void finalizeSessionReportSync(long sessionId) {
        InterviewSessionEntity session = dao.getSessionSync(sessionId);
        if (session == null) return;

        List<InterviewQuestionEntity> questions = dao.getQuestionsForSessionSync(sessionId);
        List<Pair<String, String>> qaPairs = new ArrayList<>();
        List<EvaluatedAnswerResult> evaluations = new ArrayList<>();
        int totalFillers = 0;

        for (InterviewQuestionEntity q : questions) {
            qaPairs.add(new Pair<>(q.getQuestionText(), q.getUserAnswer()));
            evaluations.add(new EvaluatedAnswerResult(
                    q.getScore(),
                    q.getTechnicalAccuracyScore(),
                    q.getCommunicationScore(),
                    q.getProblemSolvingScore(),
                    q.getConfidenceScore(),
                    q.getFeedback(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    q.getIdealAnswer()
            ));
            totalFillers += q.getFillerWordCount();
        }

        GeneratedReportResult report = geminiService.generateSessionReport(
                session.getRole(),
                session.getCompanyPreset(),
                qaPairs,
                evaluations,
                totalFillers
        );

        InterviewSessionEntity updatedSession = new InterviewSessionEntity(
                session.getId(),
                session.getUserId(),
                session.getRole(),
                session.getDifficulty(),
                session.getExperienceLevel(),
                session.getCompanyPreset(),
                session.getQuestionCount(),
                report.getOverallScore(),
                report.getVerdict(),
                report.getSummaryFeedback(),
                totalFillers,
                report.getConfidenceScore(),
                report.getTechnicalScore(),
                report.getCommunicationScore(),
                report.getProblemSolvingScore(),
                new JSONArray(report.getLearningRoadmap()).toString(),
                true,
                session.getCreatedAt()
        );

        dao.updateSession(updatedSession);
    }

    public void deleteSession(long sessionId) {
        executorService.execute(() -> dao.deleteSession(sessionId));
    }
}
