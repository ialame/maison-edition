package com.maisonedition.repository;

import com.maisonedition.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySlug(String slug);

    Page<Article> findByStatutOrderByDatePublicationDesc(Article.Statut statut, Pageable pageable);

    List<Article> findTop5ByStatutOrderByDatePublicationDesc(Article.Statut statut);

    Page<Article> findByAuteurId(Long auteurId, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a JOIN a.tags t WHERE t.slug = :tagSlug AND a.statut = :statut ORDER BY a.datePublication DESC")
    Page<Article> findByTagSlugAndStatut(@Param("tagSlug") String tagSlug, @Param("statut") Article.Statut statut, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.statut = :statut AND (LOWER(a.titre) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.chapeau) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY a.datePublication DESC")
    Page<Article> searchByTitreOrChapeau(@Param("query") String query, @Param("statut") Article.Statut statut, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a JOIN a.tags t WHERE a.statut = :statut AND a.id != :articleId AND t IN (SELECT t2 FROM Article a2 JOIN a2.tags t2 WHERE a2.id = :articleId) ORDER BY a.datePublication DESC")
    List<Article> findRelatedArticles(@Param("articleId") Long articleId, @Param("statut") Article.Statut statut, Pageable pageable);
}
