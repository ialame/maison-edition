package com.maisonedition.repository;

import com.maisonedition.entity.Evenement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {

    @Query("SELECT e FROM Evenement e WHERE e.dateDebut >= :maintenant AND e.actif = true ORDER BY e.dateDebut ASC")
    List<Evenement> findEvenementsAVenir(@Param("maintenant") LocalDateTime maintenant, Pageable pageable);

    @Query("SELECT e FROM Evenement e WHERE e.dateDebut < :maintenant AND e.actif = true ORDER BY e.dateDebut DESC")
    Page<Evenement> findEvenementsPasses(@Param("maintenant") LocalDateTime maintenant, Pageable pageable);

    List<Evenement> findByActifTrueOrderByDateDebutAsc();

    List<Evenement> findByTypeAndActifTrue(Evenement.TypeEvenement type);
}
