package com.maisonedition.dto;

import com.maisonedition.entity.Auteur;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuteurDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String biographie;
    private String photo;
    private LocalDate dateNaissance;
    private String nationalite;
    private String siteWeb;
    private Integer nombreLivres;

    public static AuteurDTO fromEntity(Auteur auteur) {
        return AuteurDTO.builder()
                .id(auteur.getId())
                .nom(auteur.getNom())
                .prenom(auteur.getPrenom())
                .biographie(auteur.getBiographie())
                .photo(auteur.getPhoto())
                .dateNaissance(auteur.getDateNaissance())
                .nationalite(auteur.getNationalite())
                .siteWeb(auteur.getSiteWeb())
                .nombreLivres(auteur.getLivres() != null ? auteur.getLivres().size() : 0)
                .build();
    }
}
