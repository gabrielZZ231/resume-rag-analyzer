package org.acme.notification.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;

@Entity
public class Notification extends PanacheEntity {

    public String message;
    public String referenceId;
    public boolean isRead = false;
    public LocalDateTime createdAt = LocalDateTime.now();

    public static long countUnread() {
        return count("isRead", false);
    }
}
