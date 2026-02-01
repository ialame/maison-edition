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
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                log.info("Session deserialized: {}", session != null);
                if (session != null) {
                    log.info("Processing payment for session: {}, paymentIntent: {}",
                            session.getId(), session.getPaymentIntent());
                    commandeService.markAsPaid(session.getId(), session.getPaymentIntent());
                    log.info("Payment confirmed for session: {}", session.getId());
                } else {
                    log.warn("Session is null after deserialization");
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
}
