package org.acme.analysis.domain;

import java.util.List;

public record ResumeData(
    PersonalInfo personalInfo,
    List<String> hardSkills,
    List<String> softSkills,
    List<String> certifications,
    List<Project> projects,
    List<String> languages
) {
    public record PersonalInfo(
        String role,
        int yearsOfExperience
    ) {}

    public record Project(
        String title,
        List<String> technologiesUsed,
        String description
    ) {}
}
