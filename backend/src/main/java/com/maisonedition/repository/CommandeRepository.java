package com.maisonedition.repository;

import com.maisonedition.entity.Commande;
import com.maisonedition.entity.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maisonedition.entity.TypeCommande;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    List<Commande> findByUtilisateurIdOrderByDateCommandeDesc(Long utilisateurId);

    Optional<Commande> findByStripeSessionId(String stripeSessionId);

    List<Commande> findByUtilisateurIdAndLivreIdAndStatut(Long utilisateurId, Long livreId, StatutCommande statut);

    /**
     * Vérifie si l'utilisateur a accès au livre via un achat (PAPIER ou NUMERIQUE) payé
     */
    @Query("SELECT COUNT(c) > 0 FROM Commande c WHERE c.utilisateur.id = :utilisateurId " +
           "AND c.livre.id = :livreId " +
           "AND c.statut = 'PAYEE' " +
           "AND c.type IN ('PAPIER', 'NUMERIQUE')")
    boolean hasAchatAccess(@Param("utilisateurId") Long utilisateurId, @Param("livreId") Long livreId);

    /**
     * Vérifie si l'utilisateur a un abonnement LECTURE_LIVRE actif pour le livre spécifique
     */
    @Query("SELECT COUNT(c) > 0 FROM Commande c WHERE c.utilisateur.id = :utilisateurId " +
           "AND c.livre.id = :livreId " +
           "AND c.statut = 'PAYEE' " +
           "AND c.type = 'LECTURE_LIVRE' " +
           "AND c.dateDebutAcces <= :today " +
           "AND c.dateFinAcces >= :today")
    boolean hasLectureLivreActif(@Param("utilisateurId") Long utilisateurId,
                                  @Param("livreId") Long livreId,
                                  @Param("today") LocalDate today);

    /**
     * Vérifie si l'utilisateur a un abonnement global actif (tous les livres)
     */
    @Query("SELECT COUNT(c) > 0 FROM Commande c WHERE c.utilisateur.id = :utilisateurId " +
           "AND c.statut = 'PAYEE' " +
           "AND c.type IN ('ABONNEMENT_MENSUEL', 'ABONNEMENT_ANNUEL') " +
           "AND c.dateDebutAcces <= :today " +
           "AND c.dateFinAcces >= :today")
    boolean hasAbonnementGlobalActif(@Param("utilisateurId") Long utilisateurId,
                                      @Param("today") LocalDate today);

    /**
     * Find existing pending order for the same item/type to avoid duplicates
     */
    @Query("SELECT c FROM Commande c WHERE c.utilisateur.id = :utilisateurId " +
           "AND c.statut = 'EN_ATTENTE' " +
           "AND c.type = :type " +
           "AND ((:livreId IS NULL AND c.livre IS NULL) OR c.livre.id = :livreId)")
    Optional<Commande> findPendingOrder(@Param("utilisateurId") Long utilisateurId,
                                        @Param("livreId") Long livreId,
                                        @Param("type") TypeCommande type);
}
