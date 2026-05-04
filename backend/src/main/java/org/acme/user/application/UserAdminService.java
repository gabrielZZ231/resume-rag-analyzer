package org.acme.user.application;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.auth.domain.VerificationToken;
import org.acme.notification.adapter.out.EmailService;
import org.acme.notification.domain.Notification;
import org.acme.user.domain.User;

import java.util.List;

@ApplicationScoped
public class UserAdminService {

    @Inject
    EmailService emailService;

    public List<User> getPendingUsers() {
        return User.find("status = ?1 and deleted = false", User.Status.PENDING_APPROVAL).list();
    }

    public List<User> getApprovedUsers() {
        return User.find("status = ?1 and deleted = false", User.Status.APPROVED).list();
    }

    @Transactional
    public boolean approveUser(Long id) {
        User user = User.findById(id);
        if (user == null || user.deleted) return false;

        user.status = User.Status.APPROVED;
        user.role = "USER";
        user.persist();

        Notification.delete("referenceId", user.id.toString());
        emailService.sendStatusEmail(user.email, "APPROVED");
        return true;
    }

    @Transactional
    public boolean rejectUser(Long id) {
        User user = User.findById(id);
        if (user == null) return false;

        String userEmail = user.email;
        String userIdStr = user.id.toString();

        VerificationToken.delete("user", user);
        Notification.delete("referenceId", userIdStr);
        user.delete();

        emailService.sendStatusEmail(userEmail, "REJECTED");
        return true;
    }

    @Transactional
    public boolean updateRole(Long id, String role) {
        User user = User.findById(id);
        if (user == null || user.deleted) return false;

        user.role = role;
        user.persist();
        return true;
    }

    @Transactional
    public boolean deleteUser(Long id) {
        User user = User.findById(id);
        if (user == null) return false;

        user.deleted = true;
        user.status = User.Status.REJECTED;
        user.role = "";
        user.password = BcryptUtil.bcryptHash(java.util.UUID.randomUUID().toString());
        user.persist();
        return true;
    }
}
