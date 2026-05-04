package org.acme.auth.adapter.out;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import io.smallrye.mutiny.Uni;
import org.acme.user.domain.User;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserStatusAugmentor implements SecurityIdentityAugmentor {

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        return context.runBlocking(() -> {
            return fetchAndAugment(identity);
        });
    }

    @ActivateRequestContext
    @Transactional
    protected SecurityIdentity fetchAndAugment(SecurityIdentity identity) {
        User user = User.findByEmail(identity.getPrincipal().getName());
        
        if (user == null) {
            
            
            return QuarkusSecurityIdentity.builder()
                    .setPrincipal(identity.getPrincipal())
                    .addCredentials(identity.getCredentials())
                    .addAttribute("accountStatus", "MISSING")
                    .build();
        }

        if (user.status != User.Status.APPROVED) {
            
            
            return QuarkusSecurityIdentity.builder(identity)
                    .setPrincipal(identity.getPrincipal())
                    .addRoles(java.util.Collections.singleton("PENDING"))
                    .build();
        }
        
        return identity;
    }
}
