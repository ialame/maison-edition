package com.maisonedition.controller;

import com.maisonedition.dto.CommandeDTO;
import com.maisonedition.entity.Commande;
import com.maisonedition.entity.StatutCommande;
import com.maisonedition.repository.CommandeRepository;
import com.maisonedition.service.CommandeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/commandes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
public class CommandeAdminController {

    private final CommandeRepository commandeRepository;
    private final CommandeService commandeService;

    @GetMapping
    public ResponseEntity<List<CommandeAdminDTO>> getAllCommandes() {
        List<CommandeAdminDTO> commandes = commandeRepository.findAll().stream()
                .sorted((a, b) -> b.getDateCommande().compareTo(a.getDateCommande()))
                .map(CommandeAdminDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(commandes);
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<?> updateStatut(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String statutStr = body.get("statut");
        if (statutStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Statut requis"));
        }

        try {
            StatutCommande statut = StatutCommande.valueOf(statutStr);
            // Use CommandeService to update status - it handles sending emails for shipping notifications
            CommandeDTO updated = commandeService.updateStatus(id, statut);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Statut invalide"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/tracking")
    public ResponseEntity<?> updateTracking(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String numeroSuivi = body.get("numeroSuivi");
        String transporteur = body.get("transporteur");

        try {
            CommandeDTO updated = commandeService.updateTracking(id, numeroSuivi, transporteur);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    public record CommandeAdminDTO(
            Long id,
            Long livreId,
            String livreTitre,
            String livreCouverture,
            Long utilisateurId,
            String utilisateurEmail,
            String utilisateurNom,
            String utilisateurPrenom,
            String type,
            String statut,
            BigDecimal montant,
            BigDecimal fraisLivraison,
            String numeroSuivi,
            String transporteur,
            String nomComplet,
            String adresse,
            String ville,
            String codePostal,
            String pays,
            String telephone,
            LocalDate dateDebutAcces,
            LocalDate dateFinAcces,
            LocalDateTime dateCommande
    ) {
        public static CommandeAdminDTO fromEntity(Commande c) {
            return new CommandeAdminDTO(
                    c.getId(),
                    c.getLivre() != null ? c.getLivre().getId() : null,
                    c.getLivre() != null ? c.getLivre().getTitre() : null,
                    c.getLivre() != null ? c.getLivre().getCouverture() : null,
                    c.getUtilisateur().getId(),
                    c.getUtilisateur().getEmail(),
                    c.getUtilisateur().getNom(),
                    c.getUtilisateur().getPrenom(),
                    c.getType().name(),
                    c.getStatut().name(),
                    c.getMontant(),
                    c.getFraisLivraison(),
                    c.getNumeroSuivi(),
                    c.getTransporteur(),
                    c.getNomComplet(),
                    c.getAdresse(),
                    c.getVille(),
                    c.getCodePostal(),
                    c.getPays(),
                    c.getTelephone(),
                    c.getDateDebutAcces(),
                    c.getDateFinAcces(),
                    c.getDateCommande()
            );
        }
    }
}
