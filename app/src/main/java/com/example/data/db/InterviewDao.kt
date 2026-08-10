package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InterviewDao {

    // User Operations
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUser(userId: String = "default_user"): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Resume Profile Operations
    @Query("SELECT * FROM resume_profiles ORDER BY createdAt DESC")
    fun getAllResumes(): Flow<List<ResumeProfileEntity>>

    @Query("SELECT * FROM resume_profiles WHERE id = :id LIMIT 1")
    suspend fun getResumeById(id: Long): ResumeProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: ResumeProfileEntity): Long

    @Query("DELETE FROM resume_profiles WHERE id = :id")
    suspend fun deleteResumeById(id: Long)

    // Interview Session Operations
    @Query("SELECT * FROM interview_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<InterviewSessionEntity>>

    @Query("SELECT * FROM interview_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionById(sessionId: Long): Flow<InterviewSessionEntity?>

    @Query("SELECT * FROM interview_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionSync(sessionId: Long): InterviewSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: InterviewSessionEntity): Long

    @Update
    suspend fun updateSession(session: InterviewSessionEntity)

    @Query("DELETE FROM interview_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    // Interview Questions Operations
    @Query("SELECT * FROM interview_questions WHERE sessionId = :sessionId ORDER BY questionIndex ASC")
    fun getQuestionsForSession(sessionId: Long): Flow<List<InterviewQuestionEntity>>

    @Query("SELECT * FROM interview_questions WHERE sessionId = :sessionId ORDER BY questionIndex ASC")
    suspend fun getQuestionsForSessionSync(sessionId: Long): List<InterviewQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: InterviewQuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<InterviewQuestionEntity>)

    @Update
    suspend fun updateQuestion(question: InterviewQuestionEntity)
}
