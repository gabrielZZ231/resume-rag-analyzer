package org.acme.auth.adapter.out;

import java.util.Set;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.smallrye.jwt.runtime.auth.JWTAuthMechanism;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SseQueryParamAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final Logger LOG = Logger.getLogger(SseQueryParamAuthenticationMechanism.class);

    private final JWTAuthMechanism jwtAuthMechanism;

    @Inject
    public SseQueryParamAuthenticationMechanism(JWTAuthMechanism jwtAuthMechanism) {
        this.jwtAuthMechanism = jwtAuthMechanism;
    }

    @Override
    public Uni<io.quarkus.security.identity.SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        String path = context.request().path();
        if (path != null && path.endsWith("/dashboard/events")) {
            String token = context.request().getParam("token");
            if (token != null && !token.isBlank() && !"null".equals(token)) {
                context.request().headers().set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                LOG.debugf("SSE Auth: token extraido da query string para %s", path);
            } else {
                LOG.warnf("SSE Auth: conexao SSE sem token em %s", path);
            }
        }

        return jwtAuthMechanism.authenticate(context, identityProviderManager);
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return jwtAuthMechanism.getChallenge(context);
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return jwtAuthMechanism.getCredentialTypes();
    }

    @Override
    public Uni<Boolean> sendChallenge(RoutingContext context) {
        return jwtAuthMechanism.sendChallenge(context);
    }

    @Override
    public HttpCredentialTransport getCredentialTransport() {
        return jwtAuthMechanism.getCredentialTransport();
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return jwtAuthMechanism.getCredentialTransport(context);
    }

    @Override
    public int getPriority() {
        return 1100;
    }
}