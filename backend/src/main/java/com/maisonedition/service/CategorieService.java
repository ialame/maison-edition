package com.maisonedition.service;

import com.maisonedition.dto.CategorieDTO;
import com.maisonedition.entity.Categorie;
import com.maisonedition.repository.CategorieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategorieService {

    private final CategorieRepository categorieRepository;

    public List<CategorieDTO> findAll() {
        return categorieRepository.findAll().stream()
                .map(CategorieDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<CategorieDTO> findRacines() {
        return categorieRepository.findByParentIsNull().stream()
                .map(CategorieDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public CategorieDTO findById(Long id) {
        return categorieRepository.findById(id)
                .map(CategorieDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + id));
    }

    public CategorieDTO findBySlug(String slug) {
        return categorieRepository.findBySlug(slug)
                .map(CategorieDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec le slug: " + slug));
    }

    public CategorieDTO create(CategorieDTO dto) {
        if (categorieRepository.existsByNom(dto.getNom())) {
            throw new RuntimeException("Une catégorie avec ce nom existe déjà");
        }

        Categorie categorie = Categorie.builder()
                .nom(dto.getNom())
                .description(dto.getDescription())
                .slug(dto.getSlug())
                .build();

        if (dto.getParentId() != null) {
            Categorie parent = categorieRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Catégorie parente non trouvée"));
            categorie.setParent(parent);
        }

        return CategorieDTO.fromEntity(categorieRepository.save(categorie));
    }

    public CategorieDTO update(Long id, CategorieDTO dto) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + id));

        categorie.setNom(dto.getNom());
        categorie.setDescription(dto.getDescription());
        categorie.setSlug(dto.getSlug());

        if (dto.getParentId() != null) {
            Categorie parent = categorieRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Catégorie parente non trouvée"));
            categorie.setParent(parent);
        } else {
            categorie.setParent(null);
        }

        return CategorieDTO.fromEntity(categorieRepository.save(categorie));
    }

    public void delete(Long id) {
        categorieRepository.deleteById(id);
    }
}
