package com.maisonedition.repository;

import com.maisonedition.entity.Chapitre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapitreRepository extends JpaRepository<Chapitre, Long> {

    List<Chapitre> findByLivreIdOrderByNumeroAsc(Long livreId);

    List<Chapitre> findByLivreIdAndPublieTrueOrderByNumeroAsc(Long livreId);

    List<Chapitre> findByLivreIdAndGratuitTrueAndPublieTrueOrderByNumeroAsc(Long livreId);

    Optional<Chapitre> findByLivreIdAndNumero(Long livreId, Integer numero);

    @Query("SELECT MAX(c.numero) FROM Chapitre c WHERE c.livre.id = :livreId")
    Integer findMaxNumeroByLivreId(@Param("livreId") Long livreId);

    long countByLivreId(Long livreId);
}
