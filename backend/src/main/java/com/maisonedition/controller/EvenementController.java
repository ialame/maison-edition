package com.maisonedition.controller;

import com.maisonedition.dto.EvenementDTO;
import com.maisonedition.entity.Evenement;
import com.maisonedition.service.EvenementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evenements")
@RequiredArgsConstructor
public class EvenementController {

    private final EvenementService evenementService;

    @GetMapping
    public ResponseEntity<List<EvenementDTO>> findAll() {
        return ResponseEntity.ok(evenementService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvenementDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(evenementService.findById(id));
    }

    @GetMapping("/a-venir")
    public ResponseEntity<List<EvenementDTO>> findAVenir(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(evenementService.findAVenir(limit));
    }

    @GetMapping("/passes")
    public ResponseEntity<Page<EvenementDTO>> findPasses(Pageable pageable) {
        return ResponseEntity.ok(evenementService.findPasses(pageable));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<EvenementDTO>> findByType(@PathVariable String type) {
        return ResponseEntity.ok(evenementService.findByType(Evenement.TypeEvenement.valueOf(type.toUpperCase())));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<EvenementDTO> create(
            @RequestBody EvenementDTO dto,
            @RequestParam(required = false) Long livreId,
            @RequestParam(required = false) Long auteurId) {
        return ResponseEntity.ok(evenementService.create(dto, livreId, auteurId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<EvenementDTO> update(
            @PathVariable Long id,
            @RequestBody EvenementDTO dto,
            @RequestParam(required = false) Long livreId,
            @RequestParam(required = false) Long auteurId) {
        return ResponseEntity.ok(evenementService.update(id, dto, livreId, auteurId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        evenementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
