package com.maisonedition.controller;

import com.maisonedition.dto.ArticleDTO;
import com.maisonedition.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<Page<ArticleDTO>> findPublies(Pageable pageable) {
        return ResponseEntity.ok(articleService.findPublies(pageable));
    }

    @GetMapping("/derniers")
    public ResponseEntity<List<ArticleDTO>> findDerniers() {
        return ResponseEntity.ok(articleService.findDerniersPublies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.findById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ArticleDTO> findBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(articleService.findBySlug(slug));
    }

    @GetMapping("/admin/tous")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<List<ArticleDTO>> findAll() {
        return ResponseEntity.ok(articleService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<ArticleDTO> create(
            @RequestBody ArticleDTO dto,
            @RequestParam(required = false) Long auteurId) {
        return ResponseEntity.ok(articleService.create(dto, auteurId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<ArticleDTO> update(@PathVariable Long id, @RequestBody ArticleDTO dto) {
        return ResponseEntity.ok(articleService.update(id, dto));
    }

    @PostMapping("/{id}/publier")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<ArticleDTO> publier(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.publier(id));
    }

    @PostMapping("/{id}/archiver")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<ArticleDTO> archiver(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.archiver(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
