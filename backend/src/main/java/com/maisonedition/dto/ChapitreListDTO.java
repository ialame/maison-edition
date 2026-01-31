package com.maisonedition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapitreListDTO {
    private Long id;
    private String titre;
    private Integer numero;
    private Boolean gratuit;
}
