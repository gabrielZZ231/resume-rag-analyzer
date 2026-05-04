package org.acme.auth.adapter.in;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.transaction.Transactional;
import org.acme.auth.domain.VerificationToken;
import org.acme.notification.domain.Notification;
import org.acme.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DeletedUserAuthenticationTest {

    @BeforeEach
    @Transactional
    void setup() {
        Notification.deleteAll();
        VerificationToken.deleteAll();
        User.deleteAll();

        User user = new User();
        user.email = "deleted-user@test.com";
        user.password = BcryptUtil.bcryptHash("secret123");
        user.role = "USER";
        user.status = User.Status.APPROVED;
        user.persist();
    }

    @Test
    @TestSecurity(user = "admin@test.com", roles = {"ADMIN"})
    void softDeletedUser_cannotLoginWithOldPassword() {
        User toDelete = User.findByEmail("deleted-user@test.com");

        given()
                .when().delete("/admin/users/" + toDelete.id)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .body("{\"email\": \"deleted-user@test.com\", \"password\": \"secret123\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401);
    }

}
