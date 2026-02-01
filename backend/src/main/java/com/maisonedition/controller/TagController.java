package com.maisonedition.controller;

import com.maisonedition.entity.Tag;
import com.maisonedition.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagRepository tagRepository;

    @GetMapping
    public List<Tag> getAll() {
        return tagRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tag> getById(@PathVariable Long id) {
        return tagRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<Tag> getBySlug(@PathVariable String slug) {
        return tagRepository.findBySlug(slug)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Tag> search(@RequestParam String q) {
        return tagRepository.findByNomContainingIgnoreCase(q);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<Tag> create(@RequestBody Map<String, String> request) {
        String nom = request.get("nom");
        if (nom == null || nom.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (tagRepository.findByNom(nom.trim()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        Tag tag = Tag.builder()
            .nom(nom.trim())
            .build();
        return ResponseEntity.ok(tagRepository.save(tag));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<Tag> update(@PathVariable Long id, @RequestBody Map<String, String> request) {
        return tagRepository.findById(id)
            .map(tag -> {
                String nom = request.get("nom");
                if (nom != null && !nom.trim().isEmpty()) {
                    tag.setNom(nom.trim());
                }
                return ResponseEntity.ok(tagRepository.save(tag));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (tagRepository.existsById(id)) {
            tagRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
