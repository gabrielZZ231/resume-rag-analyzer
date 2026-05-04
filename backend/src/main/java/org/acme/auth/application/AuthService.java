package org.acme.auth.application;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import org.acme.auth.adapter.in.dto.AuthRequest;
import org.acme.auth.domain.VerificationToken;
import org.acme.notification.adapter.out.EmailService;
import org.acme.notification.domain.Notification;
import org.acme.user.domain.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Inject
    EmailService emailService;

    @ConfigProperty(name = "app.jwt.issuer")
    String jwtIssuer;

    public Map<String, String> login(AuthRequest request) {
        String email = request.email().toLowerCase().trim();
        
        User user = User.findByEmail(email);
        if (user == null) {
            User deletedUser = User.findByEmailAnyStatus(email);
            if (deletedUser != null && deletedUser.deleted) {
                throw new NotAuthorizedException("Conta inativa");
            }
            throw new NotAuthorizedException("Login ou senha incorretos");
        }

        LOG.info("Tentativa de login para: " + email + " (Status atual: " + user.status + ")");

        if (!BcryptUtil.matches(request.password(), user.password)) {
            throw new NotAuthorizedException("Login ou senha incorretos");
        }

        if (user.status == User.Status.UNVERIFIED) {
            LOG.warn("Bloqueando login: E-mail não verificado para " + email);
            throw new ForbiddenException("E-mail não verificado. Por favor, verifique sua caixa de entrada.");
        }

        if (user.status == User.Status.REJECTED) {
            throw new ForbiddenException("Sua solicitação de acesso foi recusada.");
        }

        String token = Jwt.issuer(jwtIssuer)
                .upn(user.email)
                .groups(new HashSet<>(Arrays.asList(user.role.split(","))))
                .claim("status", user.status.name())
                .expiresIn(3600)
                .sign();

        return Map.of(
            "token", token, 
            "email", user.email, 
            "role", user.role,
            "status", user.status.name()
        );
    }

    @Transactional
    public void register(AuthRequest request) {
        String email = request.email().toLowerCase().trim();
        if (User.findByEmail(email) != null) {
            throw new BadRequestException("Email já cadastrado");
        }

        User user = new User();
        user.email = email;
        user.password = BcryptUtil.bcryptHash(request.password());
        user.role = "USER";
        user.status = User.Status.UNVERIFIED;
        user.persist();

        VerificationToken token = new VerificationToken(user);
        token.persist();

        LOG.info("Usuário registrado: " + email + ". Token de verificação: " + token.token);
        emailService.sendVerificationEmail(user.email, token.token);
    }

    @Transactional
    public void verifyToken(String tokenValue) {
        LOG.info("Tentando verificar token: " + tokenValue);
        VerificationToken token = VerificationToken.findByToken(tokenValue);
        if (token == null) {
            LOG.warn("Token não encontrado: " + tokenValue);
            throw new BadRequestException("Token inválido");
        }
        
        if (token.expiryDate.isBefore(LocalDateTime.now())) {
            LOG.warn("Token expirado: " + tokenValue);
            throw new BadRequestException("Token expirado");
        }

        User user = token.user;
        LOG.info("Verificando e-mail para o usuário: " + user.email);
        user.status = User.Status.PENDING_APPROVAL;
        user.persist();
        
        Notification notif = new Notification();
        notif.message = "Nova solicitação de acesso: " + user.email;
        notif.referenceId = user.id.toString();
        notif.persist();
        
        token.delete();
        LOG.info("E-mail verificado com sucesso para: " + user.email);
    }
}
