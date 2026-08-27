package com.example.data.remote;

import java.util.List;

public class ParsedResumeResult {
    private String candidateName;
    private List<String> skills;
    private List<String> languages;
    private List<String> frameworks;
    private List<String> projects;
    private String education;
    private double yearsExperience;

    public ParsedResumeResult(
            String candidateName,
            List<String> skills,
            List<String> languages,
            List<String> frameworks,
            List<String> projects,
            String education,
            double yearsExperience
    ) {
        this.candidateName = candidateName;
        this.skills = skills;
        this.languages = languages;
        this.frameworks = frameworks;
        this.projects = projects;
        this.education = education;
        this.yearsExperience = yearsExperience;
    }

    public String getCandidateName() { return candidateName; }
    public List<String> getSkills() { return skills; }
    public List<String> getLanguages() { return languages; }
    public List<String> getFrameworks() { return frameworks; }
    public List<String> getProjects() { return projects; }
    public String getEducation() { return education; }
    public double getYearsExperience() { return yearsExperience; }
}
