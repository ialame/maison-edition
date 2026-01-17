package com.maisonedition.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "livres")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(unique = true)
    private String isbn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String resume;

    private String couverture;

    @Column(precision = 10, scale = 2)
    private BigDecimal prix;

    private Integer nombrePages;

    private LocalDate datePublication;

    private String langue;

    private String format;

    @Column(nullable = false)
    @Builder.Default
    private Boolean disponible = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enVedette = false;

    @ManyToMany
    @JoinTable(
        name = "livre_auteur",
        joinColumns = @JoinColumn(name = "livre_id"),
        inverseJoinColumns = @JoinColumn(name = "auteur_id")
    )
    @Builder.Default
    private List<Auteur> auteurs = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}
