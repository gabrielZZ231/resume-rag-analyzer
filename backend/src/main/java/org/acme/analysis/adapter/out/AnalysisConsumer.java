package org.acme.analysis.adapter.out;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.data.document.Metadata;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.analysis.application.dto.AnalysisRequest;
import org.acme.analysis.domain.AnalysisJob;
import org.acme.analysis.domain.ResumeAnalysis;
import org.acme.analysis.domain.ResumeData;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

@ApplicationScoped
public class AnalysisConsumer {

    private static final Logger LOG = Logger.getLogger(AnalysisConsumer.class);

    @Inject
    ResumeAnalyzerAiService aiService;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<dev.langchain4j.data.segment.TextSegment> embeddingStore;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    AnalysisEventEmitter eventEmitter;

    private static final java.util.concurrent.Semaphore SEMAPHORE = new java.util.concurrent.Semaphore(1);

    @ConsumeEvent(value = "process-resume", blocking = true)
    public void process(AnalysisRequest request) {
        try {
            SEMAPHORE.acquire();
            
            UUID jobId = request.jobId();
            updateJobStatus(jobId, AnalysisJob.JobStatus.PROCESSING);

            AnalysisJob job = null;
            for (int i = 0; i < 3; i++) {
                job = AnalysisJob.findById(jobId);
                if (job != null) break;
                Thread.sleep(200);
            }

            if (job == null) {
                LOG.errorf("Job %s não encontrado no banco de dados após retries.", jobId);
                return;
            }

            String cleanResumeText = sanitizeText(request.resumeText());
            if (cleanResumeText.length() < 20) {
                throw new RuntimeException("O arquivo enviado não possui conteúdo legível suficiente para análise.");
            }

            ResumeData resumeData = aiService.structureResumeRaw(cleanResumeText.substring(0, Math.min(cleanResumeText.length(), 8000)));
            String structuredJsonText = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resumeData);

            
            Thread.sleep(15000); 

            String[] blocks = cleanResumeText.split("\\n\\s*\\n");
            for (String block : blocks) {
                String cleanBlock = sanitizeText(block.trim());
                if (cleanBlock.length() < 20) continue;
                var segment = dev.langchain4j.data.segment.TextSegment.from(
                    cleanBlock.substring(0, Math.min(cleanBlock.length(), 2000)), 
                    Metadata.from("resumeId", job.resumeId)
                );
                embeddingStore.add(embeddingModel.embed(segment).content(), segment);
            }

            var searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(sanitizeText(request.jobDescription())).content())
                .filter(metadataKey("resumeId").isEqualTo(job.resumeId))
                .maxResults(5).minScore(0.4).build();
            
            String relevantContext = embeddingStore.search(searchRequest).matches().stream()
                .map(m -> m.embedded().text()).distinct().reduce("", (a, b) -> a + "\n" + b);

            ResumeAnalysis analysis = aiService.analyzeRaw(sanitizeText(job.jobDescription), 
                "--- DADOS ESTRUTURADOS ---\n" + structuredJsonText + "\n\n--- CONTEXTO RAG ---\n" + sanitizeText(relevantContext));
            
            updateJobSuccess(jobId, analysis);

            
            Thread.sleep(10000); 

        } catch (Exception e) {
            LOG.errorf(e, ">>> ERRO CRÍTICO no processamento do Job %s: %s", request.jobId(), e.getMessage());
            if (request != null) updateJobError(request.jobId(), e.getMessage());
        } finally {
            SEMAPHORE.release();
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateJobStatus(UUID id, AnalysisJob.JobStatus status) {
        AnalysisJob job = AnalysisJob.findById(id);
        if (job != null) {
            job.status = status;
            eventEmitter.emit("updated");
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateJobSuccess(UUID id, ResumeAnalysis analysis) {
        AnalysisJob job = AnalysisJob.findById(id);
        if (job != null) {
            try {
                job.resultJson = objectMapper.writeValueAsString(analysis);
                job.matchScore = analysis.getNormalizedScore();
                job.status = AnalysisJob.JobStatus.COMPLETED;
                eventEmitter.emit("updated");
            } catch (Exception e) {
                LOG.error("Erro ao serializar resultado da análise", e);
                updateJobError(id, "Erro ao serializar resultado: " + e.getMessage());
            }
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateJobError(UUID id, String error) {
        AnalysisJob job = AnalysisJob.findById(id);
        if (job != null) {
            job.status = AnalysisJob.JobStatus.FAILED;
            job.resultJson = "{\"error\": \"" + error.replace("\"", "'") + "\"}";
            eventEmitter.emit("updated");
        }
    }

    private String sanitizeText(String input) {
        if (input == null || input.isBlank()) return "";
        return input.replace("\u0000", "")
                    .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                    .trim();
    }
}
