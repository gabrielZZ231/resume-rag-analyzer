package org.acme.auth.adapter.in;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PendingLoginFlowTest {

    @Test
    @TestSecurity(user = "pending@example.com", roles = {"PENDING"})
    void pendingUser_canAccessSessionStatus_butCannotAccessDashboardApi() {
        
        given()
                .when().get("/auth/session-status")
                .then()
                .statusCode(200);

        
        given()
                .when().get("/dashboard/me")
                .then()
                .statusCode(403);
    }
}
