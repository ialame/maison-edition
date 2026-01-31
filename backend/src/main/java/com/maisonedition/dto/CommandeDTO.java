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
        return CommandeDTO.builder()
                .id(commande.getId())
                .livreId(commande.getLivre().getId())
                .livreTitre(commande.getLivre().getTitre())
                .livreCouverture(commande.getLivre().getCouverture())
                .type(commande.getType().name())
                .statut(commande.getStatut().name())
                .montant(commande.getMontant())
                .nomComplet(commande.getNomComplet())
                .adresse(commande.getAdresse())
                .ville(commande.getVille())
                .codePostal(commande.getCodePostal())
                .pays(commande.getPays())
                .telephone(commande.getTelephone())
                .dateDebutAcces(commande.getDateDebutAcces())
                .dateFinAcces(commande.getDateFinAcces())
                .dateCommande(commande.getDateCommande())
                .build();
    }
}
