package com.maisonedition.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "commandes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livre_id")  // nullable for global subscriptions
    private Livre livre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCommande type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutCommande statut = StatutCommande.EN_ATTENTE;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal montant;

    private String stripeSessionId;
    private String stripePaymentIntentId;

    // Paper orders - shipping info
    private String nomComplet;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    private String ville;
    private String codePostal;
    private String pays;
    private String telephone;

    // Subscription orders - access period
    private LocalDate dateDebutAcces;
    private LocalDate dateFinAcces;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCommande;

    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        dateCommande = LocalDateTime.now();
        dateModification = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}
