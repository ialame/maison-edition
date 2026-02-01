package com.maisonedition.controller;

import com.maisonedition.dto.CheckoutRequest;
import com.maisonedition.dto.CommandeDTO;
import com.maisonedition.entity.Commande;
import com.maisonedition.service.CommandeService;
import com.maisonedition.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
@Slf4j
public class CommandeController {

    private final CommandeService commandeService;
    private final StripeService stripeService;

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckout(
            @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Commande commande = commandeService.createCommande(request, userDetails.getUsername());

            Session session = stripeService.createCheckoutSession(commande);
            commandeService.updateStripeSession(commande.getId(), session.getId());

            return ResponseEntity.ok(Map.of("checkoutUrl", session.getUrl()));
        } catch (StripeException e) {
            log.error("Stripe error creating checkout session", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la création de la session de paiement"));
        }
    }

    @GetMapping("/by-session")
    public ResponseEntity<CommandeDTO> getBySession(@RequestParam String sessionId) {
        CommandeDTO commande = commandeService.findByStripeSessionId(sessionId);
        return ResponseEntity.ok(commande);
    }

    @GetMapping("/mes-commandes")
    public ResponseEntity<List<CommandeDTO>> mesCommandes(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CommandeDTO> commandes = commandeService.findByUserEmail(userDetails.getUsername());
        return ResponseEntity.ok(commandes);
    }

    @PostMapping("/stripe-webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        log.info("Webhook received, signature header present: {}", sigHeader != null);
        try {
            Event event = stripeService.constructEvent(payload, sigHeader);
            log.info("Webhook event type: {}", event.getType());

            if ("checkout.session.completed".equals(event.getType())) {
                // Try standard deserialization first
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject().orElse(null);

                if (session != null) {
                    log.info("Processing payment for session: {}, paymentIntent: {}",
                            session.getId(), session.getPaymentIntent());
                    commandeService.markAsPaid(session.getId(), session.getPaymentIntent());
                    log.info("Payment confirmed for session: {}", session.getId());
                } else {
                    // Fallback: extract data from raw JSON
                    log.info("Session null, using raw JSON extraction");
                    com.stripe.model.StripeObject rawObject = event.getData().getObject();
                    if (rawObject != null) {
                        String sessionId = rawObject.toJson().contains("\"id\"")
                            ? extractJsonField(payload, "id") : null;
                        String paymentIntent = extractJsonField(payload, "payment_intent");

                        if (sessionId != null && sessionId.startsWith("cs_")) {
                            log.info("Extracted sessionId: {}, paymentIntent: {}", sessionId, paymentIntent);
                            commandeService.markAsPaid(sessionId, paymentIntent);
                            log.info("Payment confirmed for session: {}", sessionId);
                        } else {
                            log.warn("Could not extract session ID from payload");
                        }
                    }
                }
            }

            return ResponseEntity.ok("OK");
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error");
        }
    }

    private String extractJsonField(String json, String field) {
        try {
            if ("id".equals(field)) {
                // For session ID, look specifically for cs_test_ or cs_live_ pattern
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"id\":\\s*\"(cs_(?:test|live)_[^\"]+)\"");
                java.util.regex.Matcher m = p.matcher(json);
                if (m.find()) {
                    return m.group(1);
                }
            } else {
                String pattern = "\"" + field + "\":\\s*\"([^\"]+)\"";
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(json);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract field {} from JSON", field);
        }
        return null;
    }
}
