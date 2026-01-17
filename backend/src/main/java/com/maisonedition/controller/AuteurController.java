package com.maisonedition.controller;

import com.maisonedition.dto.AuteurDTO;
import com.maisonedition.service.AuteurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auteurs")
@RequiredArgsConstructor
public class AuteurController {

    private final AuteurService auteurService;

    @GetMapping
    public ResponseEntity<List<AuteurDTO>> findAll() {
        return ResponseEntity.ok(auteurService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuteurDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(auteurService.findById(id));
    }

    @GetMapping("/avec-livres")
    public ResponseEntity<List<AuteurDTO>> findAuteursAvecLivres() {
        return ResponseEntity.ok(auteurService.findAuteursAvecLivres());
    }

    @GetMapping("/recherche")
    public ResponseEntity<List<AuteurDTO>> rechercher(@RequestParam String q) {
        return ResponseEntity.ok(auteurService.rechercher(q));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<AuteurDTO> create(@RequestBody AuteurDTO dto) {
        return ResponseEntity.ok(auteurService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<AuteurDTO> update(@PathVariable Long id, @RequestBody AuteurDTO dto) {
        return ResponseEntity.ok(auteurService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        auteurService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
