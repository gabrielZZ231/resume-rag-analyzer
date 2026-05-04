package org.acme.analysis.adapter.in.dto;

import org.acme.analysis.domain.AnalysisJob;
import java.util.UUID;

public class AnalysisDashboardDTO {
    public UUID id;
    public String company;
    public String role;
    public String originalFileName;
    public Integer matchScore;
    public String status;

    public AnalysisDashboardDTO(AnalysisJob job) {
        this.id = job.id;
        this.company = job.company;
        this.role = job.role;
        this.originalFileName = job.originalFileName;
        this.matchScore = job.matchScore;
        this.status = job.status.name();
    }
}
