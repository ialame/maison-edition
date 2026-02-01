package com.maisonedition.dto;

import com.maisonedition.entity.Commentaire;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentaireDTO {
    private Long id;
    private String contenu;
    private String nomAuteur;
    private Long articleId;
    private String articleTitre;
    private Boolean approuve;
    private LocalDateTime dateCreation;

    public static CommentaireDTO fromEntity(Commentaire commentaire) {
        return CommentaireDTO.builder()
                .id(commentaire.getId())
                .contenu(commentaire.getContenu())
                .nomAuteur(commentaire.getNomAuteur())
                .articleId(commentaire.getArticle().getId())
                .articleTitre(commentaire.getArticle().getTitre())
                .approuve(commentaire.getApprouve())
                .dateCreation(commentaire.getDateCreation())
                .build();
    }
}
