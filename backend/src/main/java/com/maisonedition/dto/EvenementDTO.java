package com.maisonedition.dto;

import com.maisonedition.entity.Evenement;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvenementDTO {
    private Long id;
    private String titre;
    private String description;
    private String image;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private String adresse;
    private String ville;
    private String type;
    private Boolean actif;
    private Long livreId;
    private String livreTitre;
    private Long auteurId;
    private String auteurNom;

    public static EvenementDTO fromEntity(Evenement evenement) {
        return EvenementDTO.builder()
                .id(evenement.getId())
                .titre(evenement.getTitre())
                .description(evenement.getDescription())
                .image(evenement.getImage())
                .dateDebut(evenement.getDateDebut())
                .dateFin(evenement.getDateFin())
                .lieu(evenement.getLieu())
                .adresse(evenement.getAdresse())
                .ville(evenement.getVille())
                .type(evenement.getType().name())
                .actif(evenement.getActif())
                .livreId(evenement.getLivre() != null ? evenement.getLivre().getId() : null)
                .livreTitre(evenement.getLivre() != null ? evenement.getLivre().getTitre() : null)
                .auteurId(evenement.getAuteur() != null ? evenement.getAuteur().getId() : null)
                .auteurNom(evenement.getAuteur() != null ? evenement.getAuteur().getNomComplet() : null)
                .build();
    }
}
