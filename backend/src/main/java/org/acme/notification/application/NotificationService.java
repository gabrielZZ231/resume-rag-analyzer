package org.acme.notification.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.notification.domain.Notification;

import java.util.List;

@ApplicationScoped
public class NotificationService {

    public List<Notification> list() {
        return Notification.find("ORDER BY createdAt DESC").list();
    }

    public long countUnread() {
        return Notification.countUnread();
    }

    @Transactional
    public void markAsRead(Long id) {
        Notification notif = Notification.findById(id);
        if (notif != null) {
            notif.isRead = true;
        }
    }

    @Transactional
    public void markAllAsRead() {
        Notification.update("isRead = true");
    }
}
