package com.example.data

import com.example.data.db.InterviewDao
import com.example.data.db.InterviewQuestionEntity
import com.example.data.db.InterviewSessionEntity
import com.example.data.db.ResumeProfileEntity
import com.example.data.db.UserEntity
import com.example.data.remote.EvaluatedAnswerResult
import com.example.data.remote.GeminiService
import com.example.data.remote.ParsedResumeResult
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

class InterviewRepository(
    private val dao: InterviewDao,
    private val geminiService: GeminiService = GeminiService()
) {

    // User Operations
    val currentUser: Flow<UserEntity?> = dao.getUser()

    suspend fun saveUser(user: UserEntity) {
        dao.insertUser(user)
    }

    // Resume Operations
    val allResumes: Flow<List<ResumeProfileEntity>> = dao.getAllResumes()

    suspend fun getResumeById(id: Long): ResumeProfileEntity? = dao.getResumeById(id)

    suspend fun parseAndSaveResume(rawText: String, title: String = "Uploaded Resume"): Long {
        val parsed = geminiService.parseResumeText(rawText)

        val entity = ResumeProfileEntity(
            title = if (parsed.candidateName.isNotEmpty()) "${parsed.candidateName}'s Resume" else title,
            rawText = rawText,
            candidateName = parsed.candidateName,
            skillsJson = JSONArray(parsed.skills).toString(),
            languagesJson = JSONArray(parsed.languages).toString(),
            frameworksJson = JSONArray(parsed.frameworks).toString(),
            projectsJson = JSONArray(parsed.projects).toString(),
            education = parsed.education,
            yearsExperience = parsed.yearsExperience
        )

        val resumeId = dao.insertResume(entity)

        // Set active resume for user
        val user = UserEntity(
            id = "default_user",
            name = if (parsed.candidateName.isNotEmpty()) parsed.candidateName else "Candidate",
            activeResumeId = resumeId
        )
        dao.insertUser(user)

        return resumeId
    }

    suspend fun parseAndSaveResumeImage(base64Jpeg: String, title: String = "Uploaded Resume Photo"): Long {
        val parsed = geminiService.parseResumeImage(base64Jpeg)

        val entity = ResumeProfileEntity(
            title = if (parsed.candidateName.isNotEmpty()) "${parsed.candidateName}'s Resume Photo" else title,
            rawText = "Photo of Resume parsed via Gemini Vision AI.\nSkills: ${parsed.skills.joinToString()}\nLanguages: ${parsed.languages.joinToString()}\nFrameworks: ${parsed.frameworks.joinToString()}\nEducation: ${parsed.education}",
            candidateName = parsed.candidateName,
            skillsJson = JSONArray(parsed.skills).toString(),
            languagesJson = JSONArray(parsed.languages).toString(),
            frameworksJson = JSONArray(parsed.frameworks).toString(),
            projectsJson = JSONArray(parsed.projects).toString(),
            education = parsed.education,
            yearsExperience = parsed.yearsExperience
        )

        val resumeId = dao.insertResume(entity)

        // Set active resume for user
        val user = UserEntity(
            id = "default_user",
            name = if (parsed.candidateName.isNotEmpty()) parsed.candidateName else "Candidate",
            activeResumeId = resumeId
        )
        dao.insertUser(user)

        return resumeId
    }

    suspend fun deleteResume(id: Long) {
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
    ): Long {
        val sessionEntity = InterviewSessionEntity(
            role = role,
            difficulty = difficulty,
            experienceLevel = experienceLevel,
            companyPreset = companyPreset,
            questionCount = questionCount
        )

        val sessionId = dao.insertSession(sessionEntity)

        // Generate the first question right away
        val (firstQuestionText, firstCategory) = geminiService.generateQuestion(
            role = role,
            difficulty = difficulty,
            experienceLevel = experienceLevel,
            companyPreset = companyPreset,
            questionIndex = 1,
            totalQuestions = questionCount,
            resumeSkills = resumeSkills
        )

        val firstQuestion = InterviewQuestionEntity(
            sessionId = sessionId,
            questionIndex = 1,
            questionText = firstQuestionText,
            category = firstCategory
        )

        dao.insertQuestion(firstQuestion)

        return sessionId
    }

    suspend fun submitAndEvaluateAnswer(
        sessionId: Long,
        questionIndex: Int,
        questionId: Long,
        userAnswer: String,
        fillerWordCount: Int,
        durationSeconds: Int,
        resumeSkills: List<String>
    ): Pair<InterviewQuestionEntity, String?> { // Pair(EvaluatedQuestion, NextQuestionTextIfAny)
        val session = dao.getSessionSync(sessionId) ?: return Pair(
            InterviewQuestionEntity(sessionId = sessionId, questionIndex = questionIndex, questionText = ""), null
        )

        val existingQuestions = dao.getQuestionsForSessionSync(sessionId)
        val currentQuestionObj = existingQuestions.find { it.id == questionId }
            ?: existingQuestions.find { it.questionIndex == questionIndex }

        val questionText = currentQuestionObj?.questionText ?: ""

        val evaluation = geminiService.evaluateAnswer(
            role = session.role,
            question = questionText,
            userAnswer = userAnswer,
            resumeSkills = resumeSkills,
            fillerWordCount = fillerWordCount
        )

        val updatedQuestion = (currentQuestionObj ?: InterviewQuestionEntity(
            sessionId = sessionId,
            questionIndex = questionIndex,
            questionText = questionText
        )).copy(
            userAnswer = userAnswer,
            feedback = evaluation.feedback,
            score = evaluation.overallScore,
            technicalAccuracyScore = evaluation.technicalAccuracy,
            communicationScore = evaluation.communication,
            problemSolvingScore = evaluation.problemSolving,
            confidenceScore = evaluation.confidence,
            strengthsJson = JSONArray(evaluation.strengths).toString(),
            weaknessesJson = JSONArray(evaluation.weaknesses).toString(),
            idealAnswer = evaluation.idealAnswer,
            fillerWordCount = fillerWordCount,
            durationSeconds = durationSeconds,
            isEvaluated = true
        )

        dao.updateQuestion(updatedQuestion)

        // Check if there are more questions to generate
        var nextQuestionText: String? = null
        if (questionIndex < session.questionCount) {
            val nextIndex = questionIndex + 1
            val (nextQText, nextCat) = geminiService.generateQuestion(
                role = session.role,
                difficulty = session.difficulty,
                experienceLevel = session.experienceLevel,
                companyPreset = session.companyPreset,
                questionIndex = nextIndex,
                totalQuestions = session.questionCount,
                resumeSkills = resumeSkills,
                previousQuestion = questionText,
                previousAnswer = userAnswer
            )

            val nextQuestionObj = InterviewQuestionEntity(
                sessionId = sessionId,
                questionIndex = nextIndex,
                questionText = nextQText,
                category = nextCat
            )

            dao.insertQuestion(nextQuestionObj)
            nextQuestionText = nextQText
        } else {
            // All questions answered, generate session report & scorecard!
            finalizeSessionReport(sessionId)
        }

        return Pair(updatedQuestion, nextQuestionText)
    }

    private suspend fun finalizeSessionReport(sessionId: Long) {
        val session = dao.getSessionSync(sessionId) ?: return
        val questions = dao.getQuestionsForSessionSync(sessionId)

        val qaPairs = questions.map { Pair(it.questionText, it.userAnswer) }
        val evaluations = questions.map { q ->
            EvaluatedAnswerResult(
                overallScore = q.score,
                technicalAccuracy = q.technicalAccuracyScore,
                communication = q.communicationScore,
                problemSolving = q.problemSolvingScore,
                confidence = q.confidenceScore,
                feedback = q.feedback,
                strengths = emptyList(),
                weaknesses = emptyList(),
                idealAnswer = q.idealAnswer
            )
        }

        val totalFillers = questions.sumOf { it.fillerWordCount }

        val report = geminiService.generateSessionReport(
            role = session.role,
            companyPreset = session.companyPreset,
            questionsAndAnswers = qaPairs,
            evaluations = evaluations,
            totalFillerWords = totalFillers
        )

        val updatedSession = session.copy(
            overallScore = report.overallScore,
            verdict = report.verdict,
            summaryFeedback = report.summaryFeedback,
            fillerWordTotal = totalFillers,
            confidenceAvgScore = report.confidenceScore,
            technicalScore = report.technicalScore,
            communicationScore = report.communicationScore,
            problemSolvingScore = report.problemSolvingScore,
            roadmapJson = JSONArray(report.learningRoadmap).toString(),
            isCompleted = true
        )

        dao.updateSession(updatedSession)
    }

    suspend fun deleteSession(sessionId: Long) {
        dao.deleteSession(sessionId)
    }
}
