package org.acme.analysis.adapter.in;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import jakarta.transaction.Transactional;
import org.acme.analysis.domain.AnalysisJob;
import org.acme.auth.domain.VerificationToken;
import org.acme.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;

@QuarkusTest
public class DashboardResourceTest {

    @BeforeEach
    @Transactional
    public void setup() {
        VerificationToken.deleteAll();
        AnalysisJob.deleteAll();
        User.deleteAll();
        
        User user = new User();
        user.email = "dashboarduser";
        user.status = User.Status.APPROVED;
        user.role = "USER";
        user.persist();

        AnalysisJob job1 = new AnalysisJob();
        job1.resumeId = UUID.randomUUID().toString();
        job1.company = "Google";
        job1.role = "Dev";
        job1.originalFileName = "cv1.pdf";
        job1.matchScore = 90;
        job1.status = AnalysisJob.JobStatus.COMPLETED;
        job1.persist();

        AnalysisJob job2 = new AnalysisJob();
        job2.resumeId = UUID.randomUUID().toString();
        job2.company = "Google";
        job2.role = "Dev";
        job2.originalFileName = "cv2.pdf";
        job2.matchScore = 95;
        job2.status = AnalysisJob.JobStatus.COMPLETED;
        job2.persist();

        AnalysisJob job3 = new AnalysisJob();
        job3.resumeId = UUID.randomUUID().toString();
        job3.company = "Meta";
        job3.role = "Manager";
        job3.originalFileName = "cv3.pdf";
        job3.matchScore = 80;
        job3.status = AnalysisJob.JobStatus.COMPLETED;
        job3.persist();
    }

    @Test
    @TestSecurity(user = "dashboarduser", roles = {"USER"})
    public void testDashboardGrouping() {
        RestAssured.given()
            .when().get("/dashboard/jobs")
            .then()
            .statusCode(200)
            .body("size()", is(2))
            .body("Google.Dev", hasSize(2))
            .body("Google.Dev[0].matchScore", is(95))
            .body("Meta.Manager", hasSize(1));
    }
}
