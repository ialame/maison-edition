package com.maisonedition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapitreDTO {
    private Long id;
    private String titre;
    private String contenu;
    private Integer numero;
    private Boolean gratuit;
    private Boolean publie;
    private Long livreId;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}
