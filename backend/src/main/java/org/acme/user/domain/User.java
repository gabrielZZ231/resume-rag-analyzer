package org.acme.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    public String email;

    @JsonIgnore
    public String password;

    public String role;

    @Enumerated(EnumType.STRING)
    public Status status;

    public boolean deleted = false;

    public enum Status {
        UNVERIFIED, PENDING_APPROVAL, APPROVED, REJECTED
    }

    public static User findByEmail(String email) {
        if (email == null) return null;
        return find("email = ?1 and deleted = false", email.toLowerCase().trim()).firstResult();
    }

    public static User findByEmailAnyStatus(String email) {
        if (email == null) return null;
        return find("email = ?1", email.toLowerCase().trim()).firstResult();
    }
}
