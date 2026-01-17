package com.maisonedition.controller;

import com.maisonedition.dto.LivreDTO;
import com.maisonedition.service.LivreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/livres")
@RequiredArgsConstructor
public class LivreController {

    private final LivreService livreService;

    @GetMapping
    public ResponseEntity<Page<LivreDTO>> findAll(Pageable pageable) {
        return ResponseEntity.ok(livreService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LivreDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(livreService.findById(id));
    }

    @GetMapping("/vedette")
    public ResponseEntity<List<LivreDTO>> findEnVedette() {
        return ResponseEntity.ok(livreService.findEnVedette());
    }

    @GetMapping("/nouveautes")
    public ResponseEntity<List<LivreDTO>> findNouveautes(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(livreService.findNouveautes(limit));
    }

    @GetMapping("/categorie/{categorieId}")
    public ResponseEntity<Page<LivreDTO>> findByCategorie(@PathVariable Long categorieId, Pageable pageable) {
        return ResponseEntity.ok(livreService.findByCategorie(categorieId, pageable));
    }

    @GetMapping("/auteur/{auteurId}")
    public ResponseEntity<List<LivreDTO>> findByAuteur(@PathVariable Long auteurId) {
        return ResponseEntity.ok(livreService.findByAuteur(auteurId));
    }

    @GetMapping("/recherche")
    public ResponseEntity<Page<LivreDTO>> rechercher(@RequestParam String q, Pageable pageable) {
        return ResponseEntity.ok(livreService.rechercher(q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<LivreDTO> create(
            @RequestBody LivreDTO dto,
            @RequestParam(required = false) List<Long> auteurIds,
            @RequestParam(required = false) Long categorieId) {
        return ResponseEntity.ok(livreService.create(dto, auteurIds, categorieId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<LivreDTO> update(
            @PathVariable Long id,
            @RequestBody LivreDTO dto,
            @RequestParam(required = false) List<Long> auteurIds,
            @RequestParam(required = false) Long categorieId) {
        return ResponseEntity.ok(livreService.update(id, dto, auteurIds, categorieId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        livreService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
