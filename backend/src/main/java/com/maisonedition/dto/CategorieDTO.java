package com.maisonedition.dto;

import com.maisonedition.entity.Categorie;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorieDTO {
    private Long id;
    private String nom;
    private String description;
    private String slug;
    private Long parentId;
    private List<CategorieDTO> sousCategories;
    private Integer nombreLivres;

    public static CategorieDTO fromEntity(Categorie categorie) {
        return CategorieDTO.builder()
                .id(categorie.getId())
                .nom(categorie.getNom())
                .description(categorie.getDescription())
                .slug(categorie.getSlug())
                .parentId(categorie.getParent() != null ? categorie.getParent().getId() : null)
                .sousCategories(categorie.getSousCategories() != null ?
                        categorie.getSousCategories().stream().map(CategorieDTO::fromEntity).collect(Collectors.toList()) : null)
                .nombreLivres(categorie.getLivres() != null ? categorie.getLivres().size() : 0)
                .build();
    }
}
