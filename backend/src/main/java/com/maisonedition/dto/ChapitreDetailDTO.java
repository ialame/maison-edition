package com.maisonedition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapitreDetailDTO {
    private Long id;
    private String titre;
    private String contenu;
    private Integer numero;
    private Boolean gratuit;
    private Long livreId;
    private String livreTitre;
}
