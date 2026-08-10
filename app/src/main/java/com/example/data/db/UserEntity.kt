package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "default_user",
    val name: String = "Alex Candidate",
    val email: String = "alex@example.com",
    val currentRole: String = "Software Engineer",
    val activeResumeId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
