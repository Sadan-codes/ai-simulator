package com.example.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "resume_profiles")
public class ResumeProfileEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String userId = "default_user";
    private String title = "Main Software Resume";
    private String rawText = "";
    private String candidateName = "";
    private String skillsJson = "[]";
    private String languagesJson = "[]";
    private String frameworksJson = "[]";
    private String projectsJson = "[]";
    private String education = "";
    private double yearsExperience = 2.0;
    private long createdAt = System.currentTimeMillis();

    public ResumeProfileEntity() {}

    public ResumeProfileEntity(
            long id,
            String userId,
            String title,
            String rawText,
            String candidateName,
            String skillsJson,
            String languagesJson,
            String frameworksJson,
            String projectsJson,
            String education,
            double yearsExperience,
            long createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.rawText = rawText;
        this.candidateName = candidateName;
        this.skillsJson = skillsJson;
        this.languagesJson = languagesJson;
        this.frameworksJson = frameworksJson;
        this.projectsJson = projectsJson;
        this.education = education;
        this.yearsExperience = yearsExperience;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getSkillsJson() { return skillsJson; }
    public void setSkillsJson(String skillsJson) { this.skillsJson = skillsJson; }

    public String getLanguagesJson() { return languagesJson; }
    public void setLanguagesJson(String languagesJson) { this.languagesJson = languagesJson; }

    public String getFrameworksJson() { return frameworksJson; }
    public void setFrameworksJson(String frameworksJson) { this.frameworksJson = frameworksJson; }

    public String getProjectsJson() { return projectsJson; }
    public void setProjectsJson(String projectsJson) { this.projectsJson = projectsJson; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public double getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(double yearsExperience) { this.yearsExperience = yearsExperience; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
