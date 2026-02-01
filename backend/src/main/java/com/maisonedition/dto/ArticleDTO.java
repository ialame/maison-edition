package com.maisonedition.dto;

import com.maisonedition.entity.Article;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<TagDTO> tags;
    private List<Long> tagIds;
    private Integer tempsLecture; // in minutes
    private Long nombreVues;

    private static int calculerTempsLecture(String contenu) {
        if (contenu == null || contenu.isEmpty()) return 1;
        // Remove HTML tags
        String texte = contenu.replaceAll("<[^>]*>", " ");
        // Count words (split by whitespace)
        String[] mots = texte.trim().split("\\s+");
        int nombreMots = mots.length;
        // Arabic reading speed: ~180 words/minute
        int minutes = (int) Math.ceil(nombreMots / 180.0);
        return Math.max(1, minutes);
    }

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
                .tags(article.getTags() != null ?
                        article.getTags().stream().map(TagDTO::fromEntity).collect(Collectors.toList()) : null)
                .tempsLecture(calculerTempsLecture(article.getContenu()))
                .nombreVues(article.getNombreVues())
                .build();
    }
}
