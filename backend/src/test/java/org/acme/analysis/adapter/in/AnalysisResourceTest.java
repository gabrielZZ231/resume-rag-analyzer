package org.acme.analysis.adapter.in;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import org.acme.analysis.adapter.out.ResumeAnalyzerAiService;
import org.acme.analysis.domain.AnalysisJob;
import org.acme.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.transaction.Transactional;
import java.io.File;
import java.nio.file.Files;

import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class AnalysisResourceTest {

    @InjectMock
    ResumeAnalyzerAiService aiService;

    @BeforeEach
    @Transactional
    public void setup() {
        AnalysisJob.deleteAll();
        User.deleteAll();
        User user = new User();
        user.email = "testuser";
        user.password = "password";
        user.role = "USER";
        user.status = User.Status.APPROVED;
        user.persist();
        
        User admin = new User();
        admin.email = "admin";
        admin.password = "admin";
        admin.role = "ADMIN";
        admin.status = User.Status.APPROVED;
        admin.persist();
    }

    @Test
    public void testUnauthorizedAccess() {
        RestAssured.given()
            .when().get("/analyze")
            .then()
            .statusCode(401);
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"USER"})
    public void testBulkUpload() throws Exception {
        File tempFile1 = File.createTempFile("resume1", ".txt");
        Files.writeString(tempFile1.toPath(), "Conteúdo do currículo 1");
        
        File tempFile2 = File.createTempFile("resume2", ".txt");
        Files.writeString(tempFile2.toPath(), "Conteúdo do currículo 2");

        RestAssured.given()
            .multiPart("file", tempFile1)
            .multiPart("file", tempFile2)
            .formParam("company", "Empresa Teste")
            .formParam("role", "Desenvolvedor")
            .formParam("jobDescription", "Java Developer")
            .when().post("/analyze")
            .then()
            .statusCode(202)
            .body("size()", is(2));
            
        tempFile1.delete();
        tempFile2.delete();
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testAdminAccess() {
        RestAssured.given()
            .when().get("/admin/users")
            .then()
            .statusCode(200);
    }
}
