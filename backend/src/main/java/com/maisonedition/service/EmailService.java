package com.maisonedition.service;

import com.maisonedition.entity.Chapitre;
import com.maisonedition.entity.Commande;
import com.maisonedition.entity.Livre;
import com.maisonedition.entity.TypeCommande;
import com.maisonedition.repository.ChapitreRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final ChapitreRepository chapitreRepository;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.upload.dir}")
    private String uploadDir;

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

    @Async
    public void sendDigitalBookDelivery(Commande commande) {
        if (!mailEnabled) {
            log.info("Email disabled, skipping digital book delivery for commande {}", commande.getId());
            return;
        }

        if (commande.getLivre() == null) {
            log.warn("No book associated with commande {} for digital delivery", commande.getId());
            return;
        }

        Livre livre = commande.getLivre();
        String to = commande.getUtilisateur().getEmail();
        String customerName = commande.getUtilisateur().getPrenom() + " " + commande.getUtilisateur().getNom();
        String subject = "Votre livre numérique: " + livre.getTitre() + " - Dar Adloun";

        // Get all chapters with PDFs
        List<Chapitre> chapitres = chapitreRepository.findByLivreIdOrderByNumeroAsc(livre.getId())
                .stream()
                .filter(c -> c.getPdfPath() != null && !c.getPdfPath().isEmpty())
                .toList();

        if (chapitres.isEmpty()) {
            log.warn("No PDF chapters found for book {} in commande {}", livre.getId(), commande.getId());
            // Still send confirmation but without attachments
            String htmlContent = buildDigitalDeliveryEmail(commande, customerName, false);
            sendHtmlEmail(to, subject, htmlContent);
            return;
        }

        String htmlContent = buildDigitalDeliveryEmail(commande, customerName, true);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // Attach each chapter PDF
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            for (Chapitre chapitre : chapitres) {
                Path pdfPath = uploadPath.resolve(chapitre.getPdfPath());
                File pdfFile = pdfPath.toFile();
                if (pdfFile.exists() && pdfFile.isFile()) {
                    String attachmentName = String.format("%02d-%s.pdf", chapitre.getNumero(),
                            sanitizeFilename(chapitre.getTitre()));
                    FileSystemResource resource = new FileSystemResource(pdfFile);
                    helper.addAttachment(attachmentName, resource);
                    log.info("Attached PDF: {} for chapitre {}", attachmentName, chapitre.getId());
                } else {
                    log.warn("PDF file not found: {} for chapitre {}", pdfPath, chapitre.getId());
                }
            }

            mailSender.send(message);
            log.info("Digital book delivery email sent to {} with {} attachments", to, chapitres.size());
        } catch (MessagingException e) {
            log.error("Failed to send digital book delivery email to {}: {}", to, e.getMessage());
        }
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\u0600-\\u06FF\\s-]", "")
                   .replaceAll("\\s+", "-")
                   .substring(0, Math.min(name.length(), 50));
    }

    private String buildDigitalDeliveryEmail(Commande commande, String customerName, boolean hasAttachments) {
        String bookTitle = commande.getLivre() != null ? commande.getLivre().getTitre() : "Votre livre";

        String attachmentMessage = hasAttachments
            ? "<p>Vous trouverez en pièces jointes les fichiers PDF de votre livre. Vous pouvez également y accéder à tout moment depuis votre espace client.</p>"
            : "<p>Votre livre est accessible depuis votre espace client en ligne. Connectez-vous pour le consulter.</p>";

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
                    .highlight { background: #e8f5e9; border-left: 4px solid #4CAF50; padding: 15px; margin: 20px 0; }
                    .btn { display: inline-block; background: #813920; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .book-info { background: #f9f9f9; padding: 20px; border-radius: 8px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin: 0;">Dar Adloun</h1>
                        <p style="margin: 10px 0 0;">دار عدلون للنشر والتوزيع</p>
                    </div>
                    <div class="content">
                        <h2 style="color: #333;">Votre livre numérique est prêt!</h2>
                        <p>Bonjour %s,</p>

                        <div class="highlight">
                            <p style="margin: 0;"><strong>Bonne lecture!</strong> Votre achat a été confirmé.</p>
                        </div>

                        <div class="book-info">
                            <h3 style="color: #813920; margin-top: 0;">%s</h3>
                            <p>Numéro de commande: <strong>#%d</strong></p>
                        </div>

                        %s

                        <p style="text-align: center;">
                            <a href="%s/mes-commandes" class="btn">Accéder à mes livres</a>
                        </p>

                        <p style="margin-top: 30px;">Merci pour votre confiance!</p>
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
            bookTitle,
            commande.getId(),
            attachmentMessage,
            frontendUrl);
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
