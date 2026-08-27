package com.example.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface InterviewDao {

    // User Operations
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    Flow<UserEntity> getUser(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);

    // Resume Profile Operations
    @Query("SELECT * FROM resume_profiles ORDER BY createdAt DESC")
    Flow<List<ResumeProfileEntity>> getAllResumes();

    @Query("SELECT * FROM resume_profiles WHERE id = :id LIMIT 1")
    ResumeProfileEntity getResumeById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertResume(ResumeProfileEntity resume);

    @Query("DELETE FROM resume_profiles WHERE id = :id")
    void deleteResumeById(long id);

    // Interview Session Operations
    @Query("SELECT * FROM interview_sessions ORDER BY createdAt DESC")
    Flow<List<InterviewSessionEntity>> getAllSessions();

    @Query("SELECT * FROM interview_sessions WHERE id = :sessionId LIMIT 1")
    Flow<InterviewSessionEntity> getSessionById(long sessionId);

    @Query("SELECT * FROM interview_sessions WHERE id = :sessionId LIMIT 1")
    InterviewSessionEntity getSessionSync(long sessionId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSession(InterviewSessionEntity session);

    @Update
    void updateSession(InterviewSessionEntity session);

    @Query("DELETE FROM interview_sessions WHERE id = :sessionId")
    void deleteSession(long sessionId);

    // Interview Questions Operations
    @Query("SELECT * FROM interview_questions WHERE sessionId = :sessionId ORDER BY questionIndex ASC")
    Flow<List<InterviewQuestionEntity>> getQuestionsForSession(long sessionId);

    @Query("SELECT * FROM interview_questions WHERE sessionId = :sessionId ORDER BY questionIndex ASC")
    List<InterviewQuestionEntity> getQuestionsForSessionSync(long sessionId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertQuestion(InterviewQuestionEntity question);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQuestions(List<InterviewQuestionEntity> questions);

    @Update
    void updateQuestion(InterviewQuestionEntity question);
}
