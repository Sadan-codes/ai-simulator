package com.example.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    private String id = "default_user";
    private String name = "Alex Candidate";
    private String email = "alex@example.com";
    private String currentRole = "Software Engineer";
    private Long activeResumeId = null;
    private long createdAt = System.currentTimeMillis();

    public UserEntity() {}

    public UserEntity(@NonNull String id, String name, String email, String currentRole, Long activeResumeId, long createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.currentRole = currentRole;
        this.activeResumeId = activeResumeId;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCurrentRole() { return currentRole; }
    public void setCurrentRole(String currentRole) { this.currentRole = currentRole; }

    public Long getActiveResumeId() { return activeResumeId; }
    public void setActiveResumeId(Long activeResumeId) { this.activeResumeId = activeResumeId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
