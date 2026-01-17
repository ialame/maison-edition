package com.maisonedition.repository;

import com.maisonedition.entity.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long> {
    Optional<Categorie> findBySlug(String slug);
    List<Categorie> findByParentIsNull();
    boolean existsByNom(String nom);
}
