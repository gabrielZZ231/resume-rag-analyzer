package org.acme.auth.adapter.in;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.user.domain.User;
import org.acme.auth.adapter.in.dto.AuthRequest;
import org.acme.auth.application.AuthService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    SecurityIdentity identity;

    @ConfigProperty(name = "app.frontend.url")
    String frontendUrl;

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid AuthRequest request) {
        try {
            return Response.ok(authService.login(request)).build();
        } catch (WebApplicationException e) {
            String message = e.getMessage();
            if (message != null && message.contains("HTTP 401 Unauthorized")) {
                message = "Login ou senha incorretos";
            }
            return Response.status(e.getResponse().getStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", message != null ? message : "Erro de autenticação"))
                    .build();
        }
    }

    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid AuthRequest request) {
        authService.register(request);
        return Response.ok(Map.of("message", "Usuário registrado. Verifique seu email.")).build();
    }

    @GET
    @Path("/verify")
    @PermitAll
    @Produces(MediaType.TEXT_HTML)
    public Response verify(@QueryParam("token") String tokenValue) {
        authService.verifyToken(tokenValue);
        return Response.seeOther(URI.create(frontendUrl + "/login?verified=true")).build();
    }

    @GET
    @Path("/session-status")
    @Authenticated
    public Response sessionStatus() {
        if (identity.isAnonymous()) {
            return Response.ok(Map.of("status", "ANONYMOUS")).build();
        }

        User user = User.findByEmail(identity.getPrincipal().getName());
        if (user == null || user.deleted) {
            return Response.ok(Map.of("status", "MISSING")).build();
        }

        return Response.ok(Map.of("status", user.status.name())).build();
    }
}
