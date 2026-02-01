package com.maisonedition.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_abonnes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterAbonne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateInscription;

    private LocalDateTime dateDesinscription;

    @PrePersist
    protected void onCreate() {
        dateInscription = LocalDateTime.now();
    }
}
