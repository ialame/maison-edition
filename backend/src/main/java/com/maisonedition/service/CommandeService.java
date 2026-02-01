package com.maisonedition.service;

import com.maisonedition.dto.CheckoutRequest;
import com.maisonedition.dto.CommandeDTO;
import com.maisonedition.entity.*;
import com.maisonedition.repository.CommandeRepository;
import com.maisonedition.repository.LivreRepository;
import com.maisonedition.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final LivreRepository livreRepository;
    private final UtilisateurRepository utilisateurRepository;

    public Commande createCommande(CheckoutRequest request, String userEmail) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        TypeCommande type = TypeCommande.valueOf(request.getType());

        // Global subscriptions don't need a book
        Livre livre = null;
        Long livreId = null;
        if (type != TypeCommande.ABONNEMENT_MENSUEL && type != TypeCommande.ABONNEMENT_ANNUEL) {
            livre = livreRepository.findById(request.getLivreId())
                    .orElseThrow(() -> new RuntimeException("Livre non trouvé"));
            livreId = livre.getId();
        }

        // Check for existing pending order for the same item/type and reuse it
        Commande existingPending = commandeRepository
                .findPendingOrder(utilisateur.getId(), livreId, type)
                .orElse(null);

        if (existingPending != null) {
            // Update shipping info if this is a paper order
            if (type == TypeCommande.PAPIER) {
                existingPending.setNomComplet(request.getNomComplet());
                existingPending.setAdresse(request.getAdresse());
                existingPending.setVille(request.getVille());
                existingPending.setCodePostal(request.getCodePostal());
                existingPending.setPays(request.getPays());
                existingPending.setTelephone(request.getTelephone());
                return commandeRepository.save(existingPending);
            }
            return existingPending;
        }

        BigDecimal montant = calculateMontant(livre, type);

        Commande commande = Commande.builder()
                .utilisateur(utilisateur)
                .livre(livre)
                .type(type)
                .statut(StatutCommande.EN_ATTENTE)
                .montant(montant)
                .build();

        // Set shipping info for paper orders
        if (type == TypeCommande.PAPIER) {
            commande.setNomComplet(request.getNomComplet());
            commande.setAdresse(request.getAdresse());
            commande.setVille(request.getVille());
            commande.setCodePostal(request.getCodePostal());
            commande.setPays(request.getPays());
            commande.setTelephone(request.getTelephone());
        }

        // Set access dates for subscriptions
        if (type == TypeCommande.LECTURE_LIVRE) {
            commande.setDateDebutAcces(LocalDate.now());
            commande.setDateFinAcces(LocalDate.now().plusYears(1));
        } else if (type == TypeCommande.ABONNEMENT_MENSUEL) {
            commande.setDateDebutAcces(LocalDate.now());
            commande.setDateFinAcces(LocalDate.now().plusMonths(1));
        } else if (type == TypeCommande.ABONNEMENT_ANNUEL) {
            commande.setDateDebutAcces(LocalDate.now());
            commande.setDateFinAcces(LocalDate.now().plusYears(1));
        }

        return commandeRepository.save(commande);
    }

    public void updateStripeSession(Long commandeId, String stripeSessionId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        commande.setStripeSessionId(stripeSessionId);
        commandeRepository.save(commande);
    }

    public void markAsPaid(String stripeSessionId, String paymentIntentId) {
        commandeRepository.findByStripeSessionId(stripeSessionId).ifPresent(commande -> {
            commande.setStatut(StatutCommande.PAYEE);
            commande.setStripePaymentIntentId(paymentIntentId);
            commandeRepository.save(commande);
        });
    }

    public CommandeDTO findByStripeSessionId(String sessionId) {
        return commandeRepository.findByStripeSessionId(sessionId)
                .map(CommandeDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
    }

    public List<CommandeDTO> findByUserEmail(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return commandeRepository.findByUtilisateurIdOrderByDateCommandeDesc(utilisateur.getId())
                .stream()
                .map(CommandeDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Fixed prices in EUR
    private static final BigDecimal PRIX_NUMERIQUE = new BigDecimal("10.00");
    private static final BigDecimal PRIX_LECTURE_LIVRE = new BigDecimal("5.00");
    private static final BigDecimal PRIX_ABONNEMENT_MENSUEL = new BigDecimal("30.00");
    private static final BigDecimal PRIX_ABONNEMENT_ANNUEL = new BigDecimal("50.00");

    private BigDecimal calculateMontant(Livre livre, TypeCommande type) {
        return switch (type) {
            case PAPIER -> livre != null && livre.getPrix() != null ? livre.getPrix() : BigDecimal.ZERO;
            case NUMERIQUE -> PRIX_NUMERIQUE;
            case LECTURE_LIVRE -> PRIX_LECTURE_LIVRE;
            case ABONNEMENT_MENSUEL -> PRIX_ABONNEMENT_MENSUEL;
            case ABONNEMENT_ANNUEL -> PRIX_ABONNEMENT_ANNUEL;
        };
    }
}
