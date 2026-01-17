package com.maisonedition.dto;

import com.maisonedition.entity.Article;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleDTO {
    private Long id;
    private String titre;
    private String slug;
    private String chapeau;
    private String contenu;
    private String imagePrincipale;
    private String statut;
    private String auteurNom;
    private Long auteurId;
    private LocalDateTime datePublication;
    private LocalDateTime dateCreation;

    public static ArticleDTO fromEntity(Article article) {
        return ArticleDTO.builder()
                .id(article.getId())
                .titre(article.getTitre())
                .slug(article.getSlug())
                .chapeau(article.getChapeau())
                .contenu(article.getContenu())
                .imagePrincipale(article.getImagePrincipale())
                .statut(article.getStatut().name())
                .auteurNom(article.getAuteur() != null ?
                        article.getAuteur().getPrenom() + " " + article.getAuteur().getNom() : null)
                .auteurId(article.getAuteur() != null ? article.getAuteur().getId() : null)
                .datePublication(article.getDatePublication())
                .dateCreation(article.getDateCreation())
                .build();
    }
}
