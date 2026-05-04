package org.acme.analysis.application;

import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import org.acme.analysis.adapter.in.dto.FileUploadForm;
import org.acme.analysis.adapter.out.DocumentService;
import org.acme.analysis.application.dto.AnalysisRequest;
import org.acme.analysis.domain.AnalysisJob;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AnalysisService {

    private static final Logger LOG = Logger.getLogger(AnalysisService.class);

    @Inject
    DocumentService documentService;

    @Inject
    EventBus eventBus;

    @Inject
    TransactionSynchronizationRegistry txRegistry;

    @Transactional
    public List<AnalysisJob> startAnalysis(FileUploadForm form) {
        if (form.files == null || form.files.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo enviado");
        }

        List<AnalysisJob> jobs = new ArrayList<>();

        for (FileUpload fileUpload : form.files) {
            String extractedText = documentService.extractText(fileUpload);
            
            if (extractedText == null || extractedText.trim().isEmpty()) {
                LOG.warnf("Arquivo vazio ou ilegível: %s", fileUpload.fileName());
                continue;
            }

            AnalysisJob job = new AnalysisJob();
            job.resumeId = UUID.randomUUID().toString();
            job.jobDescription = form.jobDescription;
            job.company = form.company;
            job.role = form.role;
            job.originalFileName = fileUpload.fileName();
            job.status = AnalysisJob.JobStatus.PENDING;
            job.persistAndFlush();
            
            jobs.add(job);

            final UUID finalJobId = job.id;
            final String finalExtractedText = extractedText;
            final String finalFileName = job.originalFileName;
            final String finalJobDescription = form.jobDescription;
            final String finalCompany = form.company;
            final String finalRole = form.role;

            txRegistry.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {}

                @Override
                public void afterCompletion(int status) {
                    if (status == Status.STATUS_COMMITTED) {
                        eventBus.send("process-resume", new AnalysisRequest(
                            finalJobId,
                            finalExtractedText, 
                            finalJobDescription,
                            finalFileName,
                            finalCompany,
                            finalRole
                        ));
                    }
                }
            });
        }

        return jobs;
    }

    public AnalysisJob getById(UUID id) {
        AnalysisJob job = AnalysisJob.findById(id);
        if (job == null) {
            throw new jakarta.ws.rs.NotFoundException("Job de análise não encontrado: " + id);
        }
        return job;
    }
}
