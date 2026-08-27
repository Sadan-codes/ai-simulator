package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InterviewRepository
import com.example.data.db.AppDatabase
import com.example.data.db.InterviewQuestionEntity
import com.example.data.db.InterviewSessionEntity
import com.example.data.db.ResumeProfileEntity
import com.example.data.db.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = InterviewRepository(db.interviewDao())

    val currentUser: StateFlow<UserEntity?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allResumes: StateFlow<List<ResumeProfileEntity>> = repository.allResumes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<InterviewSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently active session state
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    private val _currentSession = MutableStateFlow<InterviewSessionEntity?>(null)
    val currentSession: StateFlow<InterviewSessionEntity?> = _currentSession.asStateFlow()

    private val _currentQuestions = MutableStateFlow<List<InterviewQuestionEntity>>(emptyList())
    val currentQuestions: StateFlow<List<InterviewQuestionEntity>> = _currentQuestions.asStateFlow()

    private val _isGeneratingQuestion = MutableStateFlow(false)
    val isGeneratingQuestion: StateFlow<Boolean> = _isGeneratingQuestion.asStateFlow()

    private val _isEvaluatingAnswer = MutableStateFlow(false)
    val isEvaluatingAnswer: StateFlow<Boolean> = _isEvaluatingAnswer.asStateFlow()

    private val _isParsingResume = MutableStateFlow(false)
    val isParsingResume: StateFlow<Boolean> = _isParsingResume.asStateFlow()

    init {
        // Ensure default user exists
        viewModelScope.launch {
            repository.saveUser(UserEntity())
        }
    }

    fun parseAndSaveResume(rawText: String, title: String = "My Resume", onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            _isParsingResume.value = true
            try {
                val resumeId = repository.parseAndSaveResume(rawText, title)
                onComplete(resumeId)
            } finally {
                _isParsingResume.value = false
            }
        }
    }

    fun parseAndSaveResumeImage(base64Jpeg: String, title: String = "Resume Photo", onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            _isParsingResume.value = true
            try {
                val resumeId = repository.parseAndSaveResumeImage(base64Jpeg, title)
                onComplete(resumeId)
            } finally {
                _isParsingResume.value = false
            }
        }
    }

    fun startNewSession(
        role: String,
        difficulty: String,
        experienceLevel: String,
        companyPreset: String,
        questionCount: Int,
        onSessionCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isGeneratingQuestion.value = true
            try {
                val activeResume = allResumes.value.firstOrNull()
                val skills = if (activeResume != null) {
                    try {
                        val jsonArr = JSONArray(activeResume.skillsJson)
                        val list = mutableListOf<String>()
                        for (i in 0 until jsonArr.length()) list.add(jsonArr.getString(i))
                        list
                    } catch (e: Exception) {
                        listOf(role, "Problem Solving")
                    }
                } else {
                    listOf(role, "Algorithms", "Software Engineering")
                }

                val sessionId = repository.createInterviewSession(
                    role,
                    difficulty,
                    experienceLevel,
                    companyPreset,
                    questionCount,
                    skills
                )

                _currentSessionId.value = sessionId
                observeSession(sessionId)
                onSessionCreated(sessionId)
            } finally {
                _isGeneratingQuestion.value = false
            }
        }
    }

    fun observeSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            repository.getSessionById(sessionId).collect { session ->
                _currentSession.value = session
            }
        }
        viewModelScope.launch {
            repository.getQuestionsForSession(sessionId).collect { questions ->
                _currentQuestions.value = questions
            }
        }
    }

    fun submitAnswer(
        questionIndex: Int,
        questionId: Long,
        answer: String,
        fillerCount: Int,
        durationSeconds: Int,
        onEvaluated: (Boolean) -> Unit // returns true if session completed
    ) {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            _isEvaluatingAnswer.value = true
            try {
                val activeResume = allResumes.value.firstOrNull()
                val skills = if (activeResume != null) {
                    try {
                        val jsonArr = JSONArray(activeResume.skillsJson)
                        val list = mutableListOf<String>()
                        for (i in 0 until jsonArr.length()) list.add(jsonArr.getString(i))
                        list
                    } catch (e: Exception) {
                        listOf("Software Architecture", "Problem Solving")
                    }
                } else {
                    listOf("Software Engineering")
                }

                val (evaluatedQ, nextQText) = repository.submitAndEvaluateAnswer(
                    sessionId,
                    questionIndex,
                    questionId,
                    answer,
                    fillerCount,
                    durationSeconds,
                    skills
                )

                val sessionCompleted = nextQText == null
                onEvaluated(sessionCompleted)
            } finally {
                _isEvaluatingAnswer.value = false
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    fun deleteResume(resumeId: Long) {
        viewModelScope.launch {
            repository.deleteResume(resumeId)
        }
    }
}
