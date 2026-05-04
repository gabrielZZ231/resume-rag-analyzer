package org.acme.core;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.acme.user.domain.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class Startup {

    @ConfigProperty(name = "app.admin.email")
    String adminEmail;

    @ConfigProperty(name = "ADMIN_PASSWORD", defaultValue = "admin123")
    String adminPassword;

    @Transactional
    public void onStart(@Observes StartupEvent ev) {

        String cleanEmail = adminEmail.toLowerCase().trim();
        User admin = User.findByEmailAnyStatus(cleanEmail);
        
        if (admin == null) {
            admin = new User();
            admin.email = cleanEmail;
        }

        admin.password = BcryptUtil.bcryptHash(adminPassword);
        admin.status = User.Status.APPROVED;
        admin.role = "ADMIN,USER";
        admin.deleted = false;
        admin.persist();
    }
}
