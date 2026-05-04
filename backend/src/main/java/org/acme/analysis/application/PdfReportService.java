package org.acme.analysis.application;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.analysis.domain.AnalysisJob;
import org.acme.analysis.domain.ResumeAnalysis;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class PdfReportService {

    @jakarta.inject.Inject
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @ConfigProperty(name = "app.report.timezone")
    String reportTimezone;

    public byte[] generateReport(String company, String role, List<AnalysisJob> jobs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);

        document.open();

        
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.GRAY);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.LIGHT_GRAY);

        
        Paragraph title = new Paragraph("Relatório de Análise de Candidatos", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph info = new Paragraph(String.format("Empresa: %s | Cargo: %s", company, role), subtitleFont);
        info.setAlignment(Element.ALIGN_CENTER);
        info.setSpacingAfter(20);
        document.add(info);

        String formattedDate = ZonedDateTime.now(ZoneId.of(reportTimezone))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Paragraph date = new Paragraph("Data do Relatório: " + formattedDate, normalFont);
        date.setSpacingAfter(20);
        document.add(date);

        
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{60, 20, 20});

        addTableHeader(table, "Nome do Arquivo", boldFont);
        addTableHeader(table, "Score", boldFont);
        addTableHeader(table, "Status", boldFont);

        for (AnalysisJob job : jobs) {
            table.addCell(new Phrase(job.originalFileName, normalFont));
            
            PdfPCell scoreCell = new PdfPCell(new Phrase(job.matchScore != null ? job.matchScore + "%" : "-", normalFont));
            scoreCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            if (job.matchScore != null) {
                if (job.matchScore >= 80) scoreCell.setBackgroundColor(new Color(209, 250, 229));
                else if (job.matchScore >= 50) scoreCell.setBackgroundColor(new Color(254, 243, 199));
                else scoreCell.setBackgroundColor(new Color(254, 226, 226));
            }
            table.addCell(scoreCell);
            
            PdfPCell statusCell = new PdfPCell(new Phrase(job.status.toString(), normalFont));
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(statusCell);

            
            if (job.resultJson != null && !job.resultJson.isEmpty() && job.analysisResult == null) {
                try {
                    job.analysisResult = objectMapper.readValue(job.resultJson, ResumeAnalysis.class);
                } catch (Exception e) {
                    
                }
            }
        }

        document.add(table);
        document.add(new Paragraph("\n"));

        
        document.add(new Paragraph("Detalhes da Análise Semântica", subtitleFont));
        document.add(new Paragraph("\n"));

        for (AnalysisJob job : jobs) {
            if (job.status == AnalysisJob.JobStatus.COMPLETED && job.analysisResult != null) {
                document.add(new Paragraph("Candidato: " + job.originalFileName, boldFont));
                document.add(new Paragraph("Score: " + job.matchScore + "%", normalFont));
                
                Paragraph summary = new Paragraph("Resumo: " + job.analysisResult.analiseGeral(), normalFont);
                summary.setSpacingBefore(5);
                summary.setSpacingAfter(10);
                document.add(summary);

                document.add(new Phrase("Principais Skills Identificadas: ", boldFont));
                StringBuilder skills = new StringBuilder();
                job.analysisResult.skillsIdentificadas().forEach(s -> skills.append(s.skillRequisitadaNaVaga()).append(", "));
                document.add(new Paragraph(skills.toString(), normalFont));

                if (!job.analysisResult.gapsDeCompetencia().isEmpty()) {
                    document.add(new Phrase("Gaps de Competência: ", boldFont));
                    document.add(new Paragraph(String.join(", ", job.analysisResult.gapsDeCompetencia()), normalFont));
                }
                
                document.add(new Paragraph("----------------------------------------------------------------------------------------------------------------------------------", footerFont));
                document.add(new Paragraph("\n"));
            }
        }

        
        Paragraph footer = new Paragraph("Gerado por Resume Intelligence RAG System", footerFont);
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void addTableHeader(PdfPTable table, String headerTitle, Font font) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(new Color(241, 245, 249));
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(headerTitle, font));
        header.setPadding(5);
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(header);
    }
}
