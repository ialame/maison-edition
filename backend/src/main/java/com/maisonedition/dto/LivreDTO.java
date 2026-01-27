package com.maisonedition.dto;

import com.maisonedition.entity.Livre;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivreDTO {
    private Long id;
    private String titre;
    private String isbn;
    private String description;
    private String resume;
    private String couverture;
    private BigDecimal prix;
    private Integer nombrePages;
    private LocalDate datePublication;
    private String langue;
    private String format;
    private String epubPath;
    private Boolean disponible;
    private Boolean enVedette;
    private List<AuteurDTO> auteurs;
    private CategorieDTO categorie;

    public static LivreDTO fromEntity(Livre livre) {
        return LivreDTO.builder()
                .id(livre.getId())
                .titre(livre.getTitre())
                .isbn(livre.getIsbn())
                .description(livre.getDescription())
                .resume(livre.getResume())
                .couverture(livre.getCouverture())
                .prix(livre.getPrix())
                .nombrePages(livre.getNombrePages())
                .datePublication(livre.getDatePublication())
                .langue(livre.getLangue())
                .format(livre.getFormat())
                .epubPath(livre.getEpubPath())
                .disponible(livre.getDisponible())
                .enVedette(livre.getEnVedette())
                .auteurs(livre.getAuteurs() != null ?
                        livre.getAuteurs().stream().map(AuteurDTO::fromEntity).collect(Collectors.toList()) : null)
                .categorie(livre.getCategorie() != null ? CategorieDTO.fromEntity(livre.getCategorie()) : null)
                .build();
    }
}
