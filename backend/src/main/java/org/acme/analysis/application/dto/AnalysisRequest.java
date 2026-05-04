package org.acme.analysis.application.dto;

import java.util.UUID;

public record AnalysisRequest(
    UUID jobId,
    String resumeText, 
    String jobDescription,
    String originalFileName,
    String company,
    String role
) {}
