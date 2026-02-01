package com.maisonedition.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "articles")
@ToString(exclude = "articles")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(unique = true)
    private String slug;

    @ManyToMany(mappedBy = "tags")
    @Builder.Default
    private Set<Article> articles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (slug == null || slug.isEmpty()) {
            slug = nom.toLowerCase()
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9\\u0600-\\u06FF]+", "-")
                .replaceAll("^-|-$", "");

            if (slug.isEmpty()) {
                slug = "tag-" + System.currentTimeMillis();
            }
        }
    }
}
