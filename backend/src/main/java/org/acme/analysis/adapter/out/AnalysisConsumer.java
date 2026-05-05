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

    private static final java.util.concurrent.Semaphore SEMAPHORE = new java.util.concurrent.Semaphore(2);

    @ConsumeEvent(value = "process-resume", blocking = true)
    public void process(AnalysisRequest request) {
        try {
            SEMAPHORE.acquire();
            
            UUID jobId = request.jobId();
            updateJobStatus(jobId, AnalysisJob.JobStatus.PROCESSING);

            AnalysisJob job = AnalysisJob.findById(jobId);
            if (job == null) {
                LOG.errorf("Job %s não encontrado no banco de dados.", jobId);
                return;
            }

            String cleanResumeText = sanitizeText(request.resumeText());
            if (cleanResumeText.length() < 20) {
                throw new RuntimeException("O arquivo enviado não possui conteúdo legível suficiente para análise.");
            }

            String cleanJobDescription = sanitizeText(request.jobDescription());

            java.util.List<String> avisosSistema = new java.util.ArrayList<>();
            
            int chunkSize = 1000;
            int overlap = 200;
            java.util.List<dev.langchain4j.data.segment.TextSegment> segments = new java.util.ArrayList<>();
            
            for (int i = 0; i < cleanResumeText.length(); i += (chunkSize - overlap)) {
                int end = Math.min(i + chunkSize, cleanResumeText.length());
                String chunk = cleanResumeText.substring(i, end).trim();
                
                String chunkLower = chunk.toLowerCase();
                if (chunk.length() < 50 || chunkLower.contains("referências") || chunkLower.contains("interesses pessoais")) {
                    continue;
                }

                segments.add(dev.langchain4j.data.segment.TextSegment.from(chunk, Metadata.from("resumeId", job.resumeId)));
                
                if (segments.size() >= 40) {
                    markAsLongResume(jobId);
                    avisosSistema.add("Atenção: Esse currículo é muito longo e apenas as partes mais relevantes foram analisadas");
                    break;
                }
                if (end == cleanResumeText.length()) break;
            }

            if (!segments.isEmpty()) {
                var embeddings = embeddingModel.embedAll(segments).content();
                embeddingStore.addAll(embeddings, segments);
            }

            int dynamicMaxResults = cleanResumeText.length() > 10000 ? 8 : 4;
            var searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(cleanJobDescription.substring(0, Math.min(cleanJobDescription.length(), 3000))).content())
                .filter(metadataKey("resumeId").isEqualTo(job.resumeId))
                .maxResults(dynamicMaxResults)
                .minScore(0.35)
                .build();
            
            String relevantContext = embeddingStore.search(searchRequest).matches().stream()
                .map(m -> m.embedded().text())
                .distinct()
                .reduce("", (a, b) -> a + "\n" + b);

            int promptLimit = 5000;
            String truncatedResume = cleanResumeText;
            if (cleanResumeText.length() > promptLimit) {
                truncatedResume = cleanResumeText.substring(0, promptLimit);
                if (!avisosSistema.contains("Atenção: Esse currículo é muito longo e apenas as partes mais relevantes foram analisadas")) {
                    markAsLongResume(jobId);
                    avisosSistema.add("Atenção: Esse currículo é muito longo e apenas as partes mais relevantes foram analisadas");
                }
            }
            
            ResumeAnalysis analysis = aiService.analyzeRaw(cleanJobDescription, 
                "--- RESUMO/TOPO DO CURRÍCULO ---\n" + truncatedResume + 
                "\n\n--- DESTAQUES SEMÂNTICOS ENCONTRADOS PELO RAG ---\n" + sanitizeText(relevantContext));
            
            ResumeAnalysis analysisWithWarnings = new ResumeAnalysis(
                analysis.matchScore(),
                analysis.analiseGeral(),
                analysis.skillsIdentificadas(),
                analysis.gapsDeCompetencia(),
                avisosSistema
            );
            
            updateJobSuccess(jobId, analysisWithWarnings);
        } catch (Exception e) {
            LOG.errorf(e, ">>> ERRO CRÍTICO no processamento do Job %s: %s", request.jobId(), e.getMessage());
            String errorMessage = e.getMessage();
            
            if (errorMessage != null) {
                if (errorMessage.toLowerCase().contains("rate limit") || errorMessage.toLowerCase().contains("quota")) {
                    errorMessage = "LIMITE_IA: O limite de processamento por minuto foi atingido. Aguarde 60 segundos antes de tentar novamente. (Dica: Se usar o plano gratuito, envie um currículo por vez).";
                } else if (errorMessage.toLowerCase().contains("context_length") || errorMessage.toLowerCase().contains("token")) {
                    errorMessage = "TAMANHO_EXCEDIDO: O texto enviado é muito grande para o modelo atual.";
                } else if (errorMessage.contains("OutputParsingException") || errorMessage.contains("JsonParseException")) {
                    errorMessage = "ERRO_FORMATO: A IA gerou uma resposta em formato inválido. Por favor, tente novamente.";
                }
            }

            if (request != null) updateJobError(request.jobId(), errorMessage);
        } finally {
            SEMAPHORE.release();
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markAsLongResume(UUID id) {
        AnalysisJob job = AnalysisJob.findById(id);
        if (job != null) {
            job.isLongResume = true;
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
