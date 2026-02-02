package com.maisonedition.dto;

import com.maisonedition.entity.Commande;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandeDTO {
    private Long id;
    private Long livreId;
    private String livreTitre;
    private String livreCouverture;
    private String type;
    private String statut;
    private BigDecimal montant;
    private BigDecimal fraisLivraison;
    private String numeroSuivi;
    private String transporteur;
    private String nomComplet;
    private String adresse;
    private String ville;
    private String codePostal;
    private String pays;
    private String telephone;
    private LocalDate dateDebutAcces;
    private LocalDate dateFinAcces;
    private LocalDateTime dateCommande;

    public static CommandeDTO fromEntity(Commande commande) {
        CommandeDTOBuilder builder = CommandeDTO.builder()
                .id(commande.getId())
                .type(commande.getType().name())
                .statut(commande.getStatut().name())
                .montant(commande.getMontant())
                .fraisLivraison(commande.getFraisLivraison())
                .numeroSuivi(commande.getNumeroSuivi())
                .transporteur(commande.getTransporteur())
                .nomComplet(commande.getNomComplet())
                .adresse(commande.getAdresse())
                .ville(commande.getVille())
                .codePostal(commande.getCodePostal())
                .pays(commande.getPays())
                .telephone(commande.getTelephone())
                .dateDebutAcces(commande.getDateDebutAcces())
                .dateFinAcces(commande.getDateFinAcces())
                .dateCommande(commande.getDateCommande());

        // Handle null livre for global subscriptions
        if (commande.getLivre() != null) {
            builder.livreId(commande.getLivre().getId())
                   .livreTitre(commande.getLivre().getTitre())
                   .livreCouverture(commande.getLivre().getCouverture());
        }

        return builder.build();
    }
}
