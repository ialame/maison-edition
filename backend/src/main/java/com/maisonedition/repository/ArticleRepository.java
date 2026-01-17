package com.maisonedition.repository;

import com.maisonedition.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySlug(String slug);

    Page<Article> findByStatutOrderByDatePublicationDesc(Article.Statut statut, Pageable pageable);

    List<Article> findTop5ByStatutOrderByDatePublicationDesc(Article.Statut statut);

    Page<Article> findByAuteurId(Long auteurId, Pageable pageable);
}
