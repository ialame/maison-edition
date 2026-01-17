package com.maisonedition.controller;

import com.maisonedition.dto.CategorieDTO;
import com.maisonedition.service.CategorieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategorieController {

    private final CategorieService categorieService;

    @GetMapping
    public ResponseEntity<List<CategorieDTO>> findAll() {
        return ResponseEntity.ok(categorieService.findAll());
    }

    @GetMapping("/racines")
    public ResponseEntity<List<CategorieDTO>> findRacines() {
        return ResponseEntity.ok(categorieService.findRacines());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategorieDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.findById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategorieDTO> findBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categorieService.findBySlug(slug));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategorieDTO> create(@RequestBody CategorieDTO dto) {
        return ResponseEntity.ok(categorieService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategorieDTO> update(@PathVariable Long id, @RequestBody CategorieDTO dto) {
        return ResponseEntity.ok(categorieService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categorieService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
