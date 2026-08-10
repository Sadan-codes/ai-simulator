package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resume_profiles")
data class ResumeProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val title: String = "Main Software Resume",
    val rawText: String,
    val candidateName: String = "",
    val skillsJson: String = "[]",          // e.g. ["React", "Kotlin", "PostgreSQL", "System Design"]
    val languagesJson: String = "[]",       // e.g. ["Kotlin", "Python", "TypeScript"]
    val frameworksJson: String = "[]",      // e.g. ["FastAPI", "Jetpack Compose", "Node.js"]
    val projectsJson: String = "[]",        // e.g. ["AI Chat App", "E-Commerce Microservice"]
    val education: String = "",
    val yearsExperience: Double = 2.0,
    val createdAt: Long = System.currentTimeMillis()
)
