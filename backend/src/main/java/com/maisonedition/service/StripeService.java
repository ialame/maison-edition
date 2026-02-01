package com.maisonedition.service;

import com.maisonedition.entity.Commande;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public Session createCheckoutSession(Commande commande) throws StripeException {
        String description = switch (commande.getType()) {
            case PAPIER -> "نسخة ورقية - " + commande.getLivre().getTitre();
            case NUMERIQUE -> "تحميل PDF - " + commande.getLivre().getTitre();
            case LECTURE_LIVRE -> "قراءة لمدة سنة - " + commande.getLivre().getTitre();
            case ABONNEMENT_MENSUEL -> "اشتراك شهري - جميع الكتب";
            case ABONNEMENT_ANNUEL -> "اشتراك سنوي - جميع الكتب";
        };

        // Cancel URL depends on whether it's a book-specific or global order
        String cancelUrl = commande.getLivre() != null
                ? frontendUrl + "/livres/" + commande.getLivre().getId() + "/commander"
                : frontendUrl + "/abonnements";

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/commande/succes?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(commande.getMontant().multiply(BigDecimal.valueOf(100)).longValue())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(description)
                                        .build())
                                .build())
                        .build())
                .putMetadata("commande_id", commande.getId().toString())
                .setCustomerEmail(commande.getUtilisateur().getEmail())
                .build();

        return Session.create(params);
    }

    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }

    public Event constructEvent(String payload, String sigHeader) throws SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret);
    }
}
