package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interview_questions")
data class InterviewQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val questionIndex: Int,             // 1, 2, 3...
    val questionText: String,
    val category: String = "Technical", // Technical, Behavioral, System Design, Live Coding
    val userAnswer: String = "",
    val feedback: String = "",
    val score: Int = 0,                 // 1 to 10
    val technicalAccuracyScore: Int = 0,
    val communicationScore: Int = 0,
    val problemSolvingScore: Int = 0,
    val confidenceScore: Int = 0,
    val strengthsJson: String = "[]",
    val weaknessesJson: String = "[]",
    val idealAnswer: String = "",
    val fillerWordCount: Int = 0,
    val durationSeconds: Int = 0,
    val isEvaluated: Boolean = false
)
