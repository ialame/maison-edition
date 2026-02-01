package com.maisonedition.dto;

import com.maisonedition.entity.Tag;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagDTO {
    private Long id;
    private String nom;
    private String slug;

    public static TagDTO fromEntity(Tag tag) {
        return TagDTO.builder()
                .id(tag.getId())
                .nom(tag.getNom())
                .slug(tag.getSlug())
                .build();
    }
}
