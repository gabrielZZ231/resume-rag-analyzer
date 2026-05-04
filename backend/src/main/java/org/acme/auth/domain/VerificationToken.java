package org.acme.auth.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import org.acme.user.domain.User;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class VerificationToken extends PanacheEntity {

    public String token;

    @OneToOne
    public User user;

    public LocalDateTime expiryDate;

    public VerificationToken() {}

    public VerificationToken(User user) {
        this.user = user;
        this.token = UUID.randomUUID().toString();
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }

    public static VerificationToken findByToken(String token) {
        return find("token", token).firstResult();
    }
}
