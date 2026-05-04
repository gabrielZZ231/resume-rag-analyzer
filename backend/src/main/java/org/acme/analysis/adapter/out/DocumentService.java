package org.acme.analysis.adapter.out;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@ApplicationScoped
public class DocumentService {

    private static final Logger LOG = Logger.getLogger(DocumentService.class);

    public String extractText(FileUpload fileUpload) {
        try {
            File actualFile = fileUpload.uploadedFile().toFile();
            String fileName = fileUpload.fileName().toLowerCase();
            String contentType = fileUpload.contentType();

            if (fileName.endsWith(".pdf") || "application/pdf".equals(contentType)) {
                try (PDDocument document = Loader.loadPDF(actualFile)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            } else if (fileName.endsWith(".docx") || contentType.contains("word")) {
                try (FileInputStream fis = new FileInputStream(actualFile);
                     XWPFDocument document = new XWPFDocument(fis);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return extractor.getText();
                }
            } else {
                byte[] bytes = Files.readAllBytes(actualFile.toPath());
                try {
                    return new String(bytes, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return new String(bytes, StandardCharsets.ISO_8859_1);
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to extract text from file: %s", fileUpload.fileName());
            throw new RuntimeException("Erro ao extrair texto do arquivo " + fileUpload.fileName(), e);
        }
    }
}
