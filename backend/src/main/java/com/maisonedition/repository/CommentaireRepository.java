package com.maisonedition.repository;

import com.maisonedition.entity.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {
    List<Commentaire> findByArticleIdAndApprouveTrue(Long articleId);
    List<Commentaire> findByArticleId(Long articleId);
    List<Commentaire> findByApprouveFalse();
    long countByApprouveFalse();
}
