package com.maisonedition.repository;

import com.maisonedition.entity.NewsletterAbonne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsletterAbonneRepository extends JpaRepository<NewsletterAbonne, Long> {
    Optional<NewsletterAbonne> findByEmail(String email);
    List<NewsletterAbonne> findByActifTrue();
    boolean existsByEmail(String email);
    long countByActifTrue();
}
