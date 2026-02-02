package com.maisonedition.service;

import com.maisonedition.entity.Commande;
import com.maisonedition.entity.TypeCommande;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Async
    public void sendOrderConfirmation(Commande commande) {
        if (!mailEnabled) {
            log.info("Email disabled, skipping order confirmation for commande {}", commande.getId());
            return;
        }

        String to = commande.getUtilisateur().getEmail();
        String customerName = commande.getUtilisateur().getPrenom() + " " + commande.getUtilisateur().getNom();
        String subject = "Confirmation de commande #" + commande.getId() + " - Dar Adloun";

        String htmlContent = buildOrderConfirmationEmail(commande, customerName);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Async
    public void sendShippingNotification(Commande commande) {
        if (!mailEnabled) {
            log.info("Email disabled, skipping shipping notification for commande {}", commande.getId());
            return;
        }

        String to = commande.getUtilisateur().getEmail();
        String customerName = commande.getUtilisateur().getPrenom() + " " + commande.getUtilisateur().getNom();
        String subject = "Votre commande #" + commande.getId() + " a été expédiée - Dar Adloun";

        String htmlContent = buildShippingNotificationEmail(commande, customerName);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Async
    public void sendTrackingNotification(Commande commande) {
        if (!mailEnabled) {
            log.info("Email disabled, skipping tracking notification for commande {}", commande.getId());
            return;
        }

        String to = commande.getUtilisateur().getEmail();
        String customerName = commande.getUtilisateur().getPrenom() + " " + commande.getUtilisateur().getNom();
        String subject = "Numéro de suivi pour votre commande #" + commande.getId() + " - Dar Adloun";

        String htmlContent = buildTrackingNotificationEmail(commande, customerName);

        sendHtmlEmail(to, subject, htmlContent);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildOrderConfirmationEmail(Commande commande, String customerName) {
        String itemDescription;
        if (commande.getLivre() != null) {
            itemDescription = commande.getLivre().getTitre();
        } else {
            itemDescription = "Abonnement Global - Accès à tous les livres";
        }

        String typeLabel = getTypeLabel(commande.getType());
        String accessPeriod = "";
        if (commande.getDateDebutAcces() != null && commande.getDateFinAcces() != null) {
            accessPeriod = String.format("""
                <tr>
                    <td style="padding: 10px; border-bottom: 1px solid #eee;">Période d'accès</td>
                    <td style="padding: 10px; border-bottom: 1px solid #eee; text-align: left;">
                        Du %s au %s
                    </td>
                </tr>
                """,
                commande.getDateDebutAcces().format(DATE_FORMATTER),
                commande.getDateFinAcces().format(DATE_FORMATTER));
        }

        String shippingAddress = "";
        if (commande.getType() == TypeCommande.PAPIER && commande.getAdresse() != null) {
            shippingAddress = String.format("""
                <h3 style="color: #813920; margin: 30px 0 15px;">Adresse de livraison</h3>
                <p style="margin: 5px 0;">%s</p>
                <p style="margin: 5px 0;">%s</p>
                <p style="margin: 5px 0;">%s %s</p>
                <p style="margin: 5px 0;">%s</p>
                """,
                commande.getNomComplet(),
                commande.getAdresse(),
                commande.getVille(), commande.getCodePostal() != null ? commande.getCodePostal() : "",
                commande.getPays() != null ? commande.getPays() : "");
        }

        return String.format("""
            <!DOCTYPE html>
            <html dir="ltr">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background: white; }
                    .header { background: linear-gradient(135deg, #813920 0%%, #a05030 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .footer { background: #f8f8f8; padding: 20px; text-align: center; font-size: 12px; color: #666; }
                    .order-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .order-table th { background: #813920; color: white; padding: 12px; text-align: right; }
                    .order-table td { padding: 12px; border-bottom: 1px solid #eee; }
                    .total { font-size: 18px; font-weight: bold; color: #813920; }
                    .btn { display: inline-block; background: #813920; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin: 0;">Dar Adloun</h1>
                        <p style="margin: 10px 0 0;">دار عدلون للنشر والتوزيع</p>
                    </div>
                    <div class="content">
                        <h2 style="color: #333;">Merci pour votre commande!</h2>
                        <p>Bonjour %s,</p>
                        <p>Nous avons bien reçu votre commande. Voici le récapitulatif :</p>

                        <h3 style="color: #813920; margin: 30px 0 15px;">Détails de la commande</h3>
                        <table class="order-table">
                            <tr>
                                <td style="padding: 10px; border-bottom: 1px solid #eee;">Numéro de commande</td>
                                <td style="padding: 10px; border-bottom: 1px solid #eee; text-align: left;"><strong>#%d</strong></td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; border-bottom: 1px solid #eee;">Article</td>
                                <td style="padding: 10px; border-bottom: 1px solid #eee; text-align: left;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px; border-bottom: 1px solid #eee;">Type</td>
                                <td style="padding: 10px; border-bottom: 1px solid #eee; text-align: left;">%s</td>
                            </tr>
                            %s
                            <tr>
                                <td style="padding: 10px; border-bottom: 1px solid #eee;"><strong>Total</strong></td>
                                <td style="padding: 10px; border-bottom: 1px solid #eee; text-align: left;" class="total">%s SAR</td>
                            </tr>
                        </table>

                        %s

                        <p style="text-align: center;">
                            <a href="%s/mes-commandes" class="btn">Voir mes commandes</a>
                        </p>

                        <p style="margin-top: 30px;">Merci de votre confiance!</p>
                        <p>L'équipe Dar Adloun</p>
                    </div>
                    <div class="footer">
                        <p>Dar Adloun - دار عدلون للنشر والتوزيع</p>
                        <p>www.dar-adloun.com | contact@dar-adloun.com</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            customerName,
            commande.getId(),
            itemDescription,
            typeLabel,
            accessPeriod,
            commande.getMontant().toString(),
            shippingAddress,
            frontendUrl);
    }

    private String buildShippingNotificationEmail(Commande commande, String customerName) {
        return String.format("""
            <!DOCTYPE html>
            <html dir="ltr">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background: white; }
                    .header { background: linear-gradient(135deg, #813920 0%%, #a05030 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .footer { background: #f8f8f8; padding: 20px; text-align: center; font-size: 12px; color: #666; }
                    .highlight { background: #f0f7e6; border-left: 4px solid #4CAF50; padding: 15px; margin: 20px 0; }
                    .btn { display: inline-block; background: #813920; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin: 0;">Dar Adloun</h1>
                        <p style="margin: 10px 0 0;">دار عدلون للنشر والتوزيع</p>
                    </div>
                    <div class="content">
                        <h2 style="color: #333;">Votre commande est en route!</h2>
                        <p>Bonjour %s,</p>

                        <div class="highlight">
                            <p style="margin: 0;"><strong>Bonne nouvelle!</strong> Votre commande #%d a été expédiée.</p>
                        </div>

                        <h3 style="color: #813920;">Article expédié</h3>
                        <p>%s</p>

                        <h3 style="color: #813920;">Adresse de livraison</h3>
                        <p style="margin: 5px 0;">%s</p>
                        <p style="margin: 5px 0;">%s</p>
                        <p style="margin: 5px 0;">%s %s</p>
                        <p style="margin: 5px 0;">%s</p>

                        <p style="text-align: center;">
                            <a href="%s/mes-commandes" class="btn">Suivre ma commande</a>
                        </p>

                        <p style="margin-top: 30px;">Merci de votre confiance!</p>
                        <p>L'équipe Dar Adloun</p>
                    </div>
                    <div class="footer">
                        <p>Dar Adloun - دار عدلون للنشر والتوزيع</p>
                        <p>www.dar-adloun.com | contact@dar-adloun.com</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            customerName,
            commande.getId(),
            commande.getLivre() != null ? commande.getLivre().getTitre() : "Votre commande",
            commande.getNomComplet() != null ? commande.getNomComplet() : customerName,
            commande.getAdresse() != null ? commande.getAdresse() : "",
            commande.getVille() != null ? commande.getVille() : "",
            commande.getCodePostal() != null ? commande.getCodePostal() : "",
            commande.getPays() != null ? commande.getPays() : "",
            frontendUrl);
    }

    private String buildTrackingNotificationEmail(Commande commande, String customerName) {
        return String.format("""
            <!DOCTYPE html>
            <html dir="ltr">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background: white; }
                    .header { background: linear-gradient(135deg, #813920 0%%, #a05030 100%%); color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .footer { background: #f8f8f8; padding: 20px; text-align: center; font-size: 12px; color: #666; }
                    .tracking-box { background: #f0f7e6; border: 2px solid #4CAF50; border-radius: 10px; padding: 20px; margin: 20px 0; text-align: center; }
                    .tracking-number { font-size: 24px; font-weight: bold; color: #333; letter-spacing: 2px; }
                    .btn { display: inline-block; background: #813920; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin: 0;">Dar Adloun</h1>
                        <p style="margin: 10px 0 0;">دار عدلون للنشر والتوزيع</p>
                    </div>
                    <div class="content">
                        <h2 style="color: #333;">Votre numéro de suivi est disponible!</h2>
                        <p>Bonjour %s,</p>
                        <p>Votre commande #%d a été expédiée. Voici votre numéro de suivi :</p>

                        <div class="tracking-box">
                            %s
                            <p class="tracking-number">%s</p>
                        </div>

                        <p style="text-align: center;">
                            <a href="%s/mes-commandes" class="btn">Voir mes commandes</a>
                        </p>

                        <p style="margin-top: 30px;">Merci de votre confiance!</p>
                        <p>L'équipe Dar Adloun</p>
                    </div>
                    <div class="footer">
                        <p>Dar Adloun - دار عدلون للنشر والتوزيع</p>
                        <p>www.dar-adloun.com | contact@dar-adloun.com</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            customerName,
            commande.getId(),
            commande.getTransporteur() != null ? "<p style='margin: 0 0 10px; color: #666;'>Transporteur: " + commande.getTransporteur() + "</p>" : "",
            commande.getNumeroSuivi(),
            frontendUrl);
    }

    private String getTypeLabel(TypeCommande type) {
        return switch (type) {
            case PAPIER -> "Livre papier";
            case NUMERIQUE -> "Livre numérique (PDF)";
            case LECTURE_LIVRE -> "Abonnement lecture (1 an)";
            case ABONNEMENT_MENSUEL -> "Abonnement mensuel";
            case ABONNEMENT_ANNUEL -> "Abonnement annuel";
        };
    }
}
