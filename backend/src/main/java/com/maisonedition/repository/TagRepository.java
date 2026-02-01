package com.maisonedition.repository;

import com.maisonedition.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findBySlug(String slug);
    Optional<Tag> findByNom(String nom);
    List<Tag> findByNomContainingIgnoreCase(String nom);
}
