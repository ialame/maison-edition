package com.maisonedition.repository;

import com.maisonedition.entity.Livre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivreRepository extends JpaRepository<Livre, Long> {
    Optional<Livre> findByIsbn(String isbn);

    List<Livre> findByEnVedetteTrue();

    List<Livre> findByDisponibleTrue();

    Page<Livre> findByCategorieId(Long categorieId, Pageable pageable);

    @Query("SELECT l FROM Livre l JOIN l.auteurs a WHERE a.id = :auteurId")
    List<Livre> findByAuteurId(@Param("auteurId") Long auteurId);

    @Query("SELECT l FROM Livre l WHERE LOWER(l.titre) LIKE LOWER(CONCAT('%', :terme, '%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%', :terme, '%'))")
    Page<Livre> rechercher(@Param("terme") String terme, Pageable pageable);

    @Query("SELECT l FROM Livre l ORDER BY l.dateCreation DESC")
    List<Livre> findDernieresNouveautes(Pageable pageable);

    @Query("SELECT l FROM Livre l WHERE l.stock <= l.seuilAlerte AND l.prix IS NOT NULL")
    List<Livre> findLivresStockBas();

    @Query("SELECT l FROM Livre l WHERE l.stock = 0 AND l.prix IS NOT NULL")
    List<Livre> findLivresRuptureStock();

    @Query("SELECT COUNT(l) FROM Livre l WHERE l.stock <= l.seuilAlerte AND l.prix IS NOT NULL")
    long countLivresStockBas();
}
