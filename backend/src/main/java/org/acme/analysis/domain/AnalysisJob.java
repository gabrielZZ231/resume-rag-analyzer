package org.acme.analysis.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;

@Entity
public class AnalysisJob extends PanacheEntityBase {

    @Id
    @GeneratedValue(generator = "UUID")
    public UUID id;

    public String resumeId;

    public String company;
    public String role;
    public String originalFileName;
    public Integer matchScore;
    public Boolean isLongResume = false;

    @Column(columnDefinition = "TEXT")
    public String jobDescription;

    @Enumerated(EnumType.STRING)
    public JobStatus status;

    @Column(columnDefinition = "TEXT")
    public String resultJson;

    @jakarta.persistence.Transient
    public ResumeAnalysis analysisResult;

    public LocalDateTime createdAt = LocalDateTime.now();

    public enum JobStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
