package com.maisonedition.repository;

import com.maisonedition.entity.Auteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuteurRepository extends JpaRepository<Auteur, Long> {
    List<Auteur> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom);

    @Query("SELECT a FROM Auteur a WHERE SIZE(a.livres) > 0 ORDER BY a.nom")
    List<Auteur> findAuteursAvecLivres();
}
