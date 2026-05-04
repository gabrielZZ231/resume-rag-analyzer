package org.acme.user.adapter.in;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.transaction.Transactional;
import org.acme.auth.domain.VerificationToken;
import org.acme.notification.domain.Notification;
import org.acme.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AdminResourceTest {

    @BeforeEach
    @Transactional
    void setup() {
        Notification.deleteAll();
        VerificationToken.deleteAll();
        User.deleteAll();

        User pending = new User();
        pending.email = "pending@test.com";
        pending.password = "secret";
        pending.role = "";
        pending.status = User.Status.PENDING_APPROVAL;
        pending.persist();

        Notification n = new Notification();
        n.message = "Nova solicitacao de acesso: " + pending.email;
        n.referenceId = pending.id.toString();
        n.persist();

        User admin = new User();
        admin.email = "admin@test.com";
        admin.password = "secret";
        admin.role = "ADMIN";
        admin.status = User.Status.APPROVED;
        admin.persist();
    }

    @Test
    @TestSecurity(user = "admin@test.com", roles = {"ADMIN"})
    void approveUser_movesUserOutOfTriage() {
                User pending = User.findByEmail("pending@test.com");
                Long pendingId = pending.id;

        given()
                .when().post("/admin/approve/" + pendingId)
                .then()
                .statusCode(200);

        given()
                .when().get("/admin/pending-users")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));

        given()
                .when().get("/admin/users")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("email", hasSize(2));
    }

    @Test
    @TestSecurity(user = "admin@test.com", roles = {"ADMIN"})
    void rejectUser_removesUserFromSystemAndTriage() {
                User pending = User.findByEmail("pending@test.com");
                Long pendingId = pending.id;

        given()
                .when().post("/admin/reject/" + pendingId)
                .then()
                .statusCode(200);

        given()
                .when().get("/admin/pending-users")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));

        given()
                .when().get("/admin/users")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }
}
