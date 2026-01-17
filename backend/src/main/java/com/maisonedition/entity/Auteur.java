package com.maisonedition.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auteurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auteur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(columnDefinition = "TEXT")
    private String biographie;

    private String photo;

    private LocalDate dateNaissance;

    private String nationalite;

    private String siteWeb;

    @ManyToMany(mappedBy = "auteurs")
    @Builder.Default
    private List<Livre> livres = new ArrayList<>();

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

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
