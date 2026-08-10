package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interview_sessions")
data class InterviewSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val role: String,                   // Frontend, Backend, Data Science, Android, System Design, etc.
    val difficulty: String,             // Easy, Medium, Hard, Big-Tech FAANG
    val experienceLevel: String,        // Intern, Junior, Mid-Level, Senior
    val companyPreset: String = "General Tech", // Google, Amazon, Meta, Microsoft, Apple, General Tech, Startup
    val questionCount: Int = 5,
    val overallScore: Int = 0,          // 0 to 100
    val verdict: String = "Pending",    // Strong Hire, Hire, Needs Improvement, Re-eval
    val summaryFeedback: String = "",
    val fillerWordTotal: Int = 0,
    val confidenceAvgScore: Int = 0,
    val technicalScore: Int = 0,
    val communicationScore: Int = 0,
    val problemSolvingScore: Int = 0,
    val roadmapJson: String = "[]",     // Personalized learning topics
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
