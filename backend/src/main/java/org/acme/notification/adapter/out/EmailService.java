package org.acme.notification.adapter.out;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "app.base-url")
    String baseUrl;

    public void sendVerificationEmail(String email, String token) {
        String verificationUrl = baseUrl + "/auth/verify?token=" + token;
        String htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 5px;">
                        <h2 style="color: #2c3e50;">Verifique seu email</h2>
                        <p>Olá!</p>
                        <p>Obrigado por se cadastrar no <strong>Resume Intelligence</strong>. Por favor, clique no botão abaixo para verificar seu email e entrar na fila de aprovação:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background-color: #3498db; color: white; padding: 12px 25px; text-decoration: none; border-radius: 3px; font-weight: bold;">Verificar Email</a>
                        </div>
                        <p>Se o botão não funcionar, copie e cole o link abaixo no seu navegador:</p>
                        <p style="word-break: break-all;"><a href="%s">%s</a></p>
                        <hr style="border: 0; border-top: 1px solid #eee; margin: 20px 0;">
                        <p style="font-size: 0.8em; color: #7f8c8d;">Esta é uma mensagem automática, por favor não responda.</p>
                    </div>
                </body>
            </html>
            """.formatted(verificationUrl, verificationUrl, verificationUrl);

        mailer.send(Mail.withHtml(email, 
            "Verifique seu email - Resume Intelligence", 
            htmlContent)
            .setText("Olá!\n\nPor favor, clique no link abaixo para verificar seu email:\n\n" + verificationUrl));
    }

    public void sendStatusEmail(String email, String status) {
        boolean approved = status.equals("APPROVED");
        String title = approved ? "Sua conta foi aprovada!" : "Status da sua solicitação";
        String message = approved ? 
            "Parabéns! Sua conta foi aprovada. Você já pode acessar o sistema e começar a analisar currículos." : 
            "Infelizmente, sua solicitação de acesso foi recusada no momento.";
        
        String htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 5px;">
                        <h2 style="color: %s;">%s</h2>
                        <p>Olá!</p>
                        <p>%s</p>
                        %s
                        <hr style="border: 0; border-top: 1px solid #eee; margin: 20px 0;">
                        <p style="font-size: 0.8em; color: #7f8c8d;">Esta é uma mensagem automática, por favor não responda.</p>
                    </div>
                </body>
            </html>
            """.formatted(
                approved ? "#27ae60" : "#c0392b",
                title,
                message,
                approved ? "<div style='text-align: center; margin: 30px 0;'><a href='" + baseUrl + "' style='background-color: #27ae60; color: white; padding: 12px 25px; text-decoration: none; border-radius: 3px; font-weight: bold;'>Acessar Sistema</a></div>" : ""
            );

        mailer.send(Mail.withHtml(email, 
            "Status da sua conta - Resume Intelligence", 
            htmlContent)
            .setText(message));
    }
}
