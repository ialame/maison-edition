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

        Livre livre = livreRepository.findById(request.getLivreId())
                .orElseThrow(() -> new RuntimeException("Livre non trouvé"));

        TypeCommande type = TypeCommande.valueOf(request.getType());
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
        if (type == TypeCommande.ABONNEMENT_MENSUEL) {
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

    private BigDecimal calculateMontant(Livre livre, TypeCommande type) {
        return switch (type) {
            case PAPIER -> livre.getPrix() != null ? livre.getPrix() : BigDecimal.ZERO;
            case NUMERIQUE -> livre.getPrixNumerique() != null ? livre.getPrixNumerique() : BigDecimal.ZERO;
            case ABONNEMENT_MENSUEL -> livre.getPrixAbonnementMensuel() != null ? livre.getPrixAbonnementMensuel() : BigDecimal.ZERO;
            case ABONNEMENT_ANNUEL -> livre.getPrixAbonnementAnnuel() != null ? livre.getPrixAbonnementAnnuel() : BigDecimal.ZERO;
        };
    }
}
