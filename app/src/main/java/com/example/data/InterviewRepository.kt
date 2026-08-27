package com.example.data

import com.example.data.db.InterviewDao
import com.example.data.db.InterviewQuestionEntity
import com.example.data.db.InterviewSessionEntity
import com.example.data.db.ResumeProfileEntity
import com.example.data.db.UserEntity
import com.example.data.remote.EvaluatedAnswerResult
import com.example.data.remote.GeminiService
import com.example.data.remote.ParsedResumeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray

class InterviewRepository(
    private val dao: InterviewDao,
    private val geminiService: GeminiService = GeminiService()
) {

    // User Operations
    val currentUser: Flow<UserEntity?> = dao.getUser("default_user")

    suspend fun saveUser(user: UserEntity) = withContext(Dispatchers.IO) {
        dao.insertUser(user)
    }

    // Resume Operations
    val allResumes: Flow<List<ResumeProfileEntity>> = dao.getAllResumes()

    suspend fun getResumeById(id: Long): ResumeProfileEntity? = withContext(Dispatchers.IO) {
        dao.getResumeById(id)
    }

    suspend fun parseAndSaveResume(rawText: String, title: String = "Uploaded Resume"): Long = withContext(Dispatchers.IO) {
        val parsed = geminiService.parseResumeText(rawText)

        val entity = ResumeProfileEntity(
            0,
            "default_user",
            if (parsed.candidateName.isNotEmpty()) "${parsed.candidateName}'s Resume" else title,
            rawText,
            parsed.candidateName,
            JSONArray(parsed.skills).toString(),
            JSONArray(parsed.languages).toString(),
            JSONArray(parsed.frameworks).toString(),
            JSONArray(parsed.projects).toString(),
            parsed.education,
            parsed.yearsExperience,
            System.currentTimeMillis()
        )

        val resumeId = dao.insertResume(entity)

        // Set active resume for user
        val user = UserEntity(
            "default_user",
            if (parsed.candidateName.isNotEmpty()) parsed.candidateName else "Candidate",
            "alex@example.com",
            "Software Engineer",
            resumeId,
            System.currentTimeMillis()
        )
        dao.insertUser(user)

        resumeId
    }

    suspend fun parseAndSaveResumeImage(base64Jpeg: String, title: String = "Uploaded Resume Photo"): Long = withContext(Dispatchers.IO) {
        val parsed = geminiService.parseResumeImage(base64Jpeg)

        val entity = ResumeProfileEntity(
            0,
            "default_user",
            if (parsed.candidateName.isNotEmpty()) "${parsed.candidateName}'s Resume Photo" else title,
            "Photo of Resume parsed via Gemini Vision AI.\nSkills: ${parsed.skills.joinToString()}\nLanguages: ${parsed.languages.joinToString()}\nFrameworks: ${parsed.frameworks.joinToString()}\nEducation: ${parsed.education}",
            parsed.candidateName,
            JSONArray(parsed.skills).toString(),
            JSONArray(parsed.languages).toString(),
            JSONArray(parsed.frameworks).toString(),
            JSONArray(parsed.projects).toString(),
            parsed.education,
            parsed.yearsExperience,
            System.currentTimeMillis()
        )

        val resumeId = dao.insertResume(entity)

        // Set active resume for user
        val user = UserEntity(
            "default_user",
            if (parsed.candidateName.isNotEmpty()) parsed.candidateName else "Candidate",
            "alex@example.com",
            "Software Engineer",
            resumeId,
            System.currentTimeMillis()
        )
        dao.insertUser(user)

        resumeId
    }

    suspend fun deleteResume(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteResumeById(id)
    }

    // Interview Session Operations
    val allSessions: Flow<List<InterviewSessionEntity>> = dao.getAllSessions()

    fun getSessionById(sessionId: Long): Flow<InterviewSessionEntity?> = dao.getSessionById(sessionId)

    fun getQuestionsForSession(sessionId: Long): Flow<List<InterviewQuestionEntity>> =
        dao.getQuestionsForSession(sessionId)

    suspend fun createInterviewSession(
        role: String,
        difficulty: String,
        experienceLevel: String,
        companyPreset: String,
        questionCount: Int = 5,
        resumeSkills: List<String>
    ): Long = withContext(Dispatchers.IO) {
        val sessionEntity = InterviewSessionEntity(
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
        )

        val sessionId = dao.insertSession(sessionEntity)

        // Generate the first question right away
        val (firstQuestionText, firstCategory) = geminiService.generateQuestion(
            role,
            difficulty,
            experienceLevel,
            companyPreset,
            1,
            questionCount,
            resumeSkills
        )

        val firstQuestion = InterviewQuestionEntity(
            0,
            sessionId,
            1,
            firstQuestionText,
            firstCategory,
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
        )

        dao.insertQuestion(firstQuestion)

        sessionId
    }

    suspend fun submitAndEvaluateAnswer(
        sessionId: Long,
        questionIndex: Int,
        questionId: Long,
        userAnswer: String,
        fillerWordCount: Int,
        durationSeconds: Int,
        resumeSkills: List<String>
    ): Pair<InterviewQuestionEntity, String?> = withContext(Dispatchers.IO) { // Pair(EvaluatedQuestion, NextQuestionTextIfAny)
        val session = dao.getSessionSync(sessionId) ?: return@withContext Pair(
            InterviewQuestionEntity(0, sessionId, questionIndex, "", "Technical", "", "", 0, 0, 0, 0, 0, "[]", "[]", "", 0, 0, false), null
        )

        val existingQuestions = dao.getQuestionsForSessionSync(sessionId)
        val currentQuestionObj = existingQuestions.find { it.id == questionId }
            ?: existingQuestions.find { it.questionIndex == questionIndex }

        val questionText = currentQuestionObj?.questionText ?: ""

        val evaluation = geminiService.evaluateAnswer(
            session.role,
            questionText,
            userAnswer,
            resumeSkills,
            fillerWordCount
        )

        val targetQId = currentQuestionObj?.id ?: 0L
        val updatedQuestion = InterviewQuestionEntity(
            targetQId,
            sessionId,
            questionIndex,
            questionText,
            currentQuestionObj?.category ?: "Technical",
            userAnswer,
            evaluation.feedback,
            evaluation.overallScore,
            evaluation.technicalAccuracy,
            evaluation.communication,
            evaluation.problemSolving,
            evaluation.confidence,
            JSONArray(evaluation.strengths).toString(),
            JSONArray(evaluation.weaknesses).toString(),
            evaluation.idealAnswer,
            fillerWordCount,
            durationSeconds,
            true
        )

        dao.updateQuestion(updatedQuestion)

        // Check if there are more questions to generate
        var nextQuestionText: String? = null
        if (questionIndex < session.questionCount) {
            val nextIndex = questionIndex + 1
            val (nextQText, nextCat) = geminiService.generateQuestion(
                session.role,
                session.difficulty,
                session.experienceLevel,
                session.companyPreset,
                nextIndex,
                session.questionCount,
                resumeSkills,
                questionText,
                userAnswer
            )

            val nextQuestionObj = InterviewQuestionEntity(
                0,
                sessionId,
                nextIndex,
                nextQText,
                nextCat,
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
            )

            dao.insertQuestion(nextQuestionObj)
            nextQuestionText = nextQText
        } else {
            // All questions answered, generate session report & scorecard!
            finalizeSessionReport(sessionId)
        }

        Pair(updatedQuestion, nextQuestionText)
    }

    private suspend fun finalizeSessionReport(sessionId: Long) = withContext(Dispatchers.IO) {
        val session = dao.getSessionSync(sessionId) ?: return@withContext
        val questions = dao.getQuestionsForSessionSync(sessionId)

        val qaPairs = questions.map { Pair(it.questionText, it.userAnswer) }
        val evaluations = questions.map { q ->
            EvaluatedAnswerResult(
                q.score,
                q.technicalAccuracyScore,
                q.communicationScore,
                q.problemSolvingScore,
                q.confidenceScore,
                q.feedback,
                emptyList(),
                emptyList(),
                q.idealAnswer
            )
        }

        val totalFillers = questions.sumOf { it.fillerWordCount }

        val report = geminiService.generateSessionReport(
            session.role,
            session.companyPreset,
            qaPairs,
            evaluations,
            totalFillers
        )

        val updatedSession = InterviewSessionEntity(
            session.id,
            session.userId,
            session.role,
            session.difficulty,
            session.experienceLevel,
            session.companyPreset,
            session.questionCount,
            report.overallScore,
            report.verdict,
            report.summaryFeedback,
            totalFillers,
            report.confidenceScore,
            report.technicalScore,
            report.communicationScore,
            report.problemSolvingScore,
            JSONArray(report.learningRoadmap).toString(),
            true,
            session.createdAt
        )

        dao.updateSession(updatedSession)
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        dao.deleteSession(sessionId)
    }
}
