package com.maisonedition.repository;

import com.maisonedition.entity.Commande;
import com.maisonedition.entity.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    List<Commande> findByUtilisateurIdOrderByDateCommandeDesc(Long utilisateurId);

    Optional<Commande> findByStripeSessionId(String stripeSessionId);

    List<Commande> findByUtilisateurIdAndLivreIdAndStatut(Long utilisateurId, Long livreId, StatutCommande statut);
}
