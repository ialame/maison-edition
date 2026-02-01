package com.maisonedition.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "articles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "tags")
@ToString(exclude = "tags")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String chapeau;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String contenu;

    private String imagePrincipale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Statut statut = Statut.BROUILLON;

    @ManyToOne
    @JoinColumn(name = "auteur_id")
    private Utilisateur auteur;

    private LocalDateTime datePublication;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    @ManyToMany
    @JoinTable(
        name = "article_tags",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
        if (slug == null || slug.isEmpty()) {
            String generatedSlug = titre.toLowerCase()
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

            // If slug is empty (e.g., Arabic title), use timestamp-based slug
            if (generatedSlug.isEmpty()) {
                slug = "article-" + System.currentTimeMillis();
            } else {
                slug = generatedSlug;
            }
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }

    public enum Statut {
        BROUILLON, PUBLIE, ARCHIVE
    }
}
