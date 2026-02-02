package com.maisonedition.controller;

import com.maisonedition.dto.LivreDTO;
import com.maisonedition.service.LivreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/livres")
@RequiredArgsConstructor
public class LivreController {

    private final LivreService livreService;

    @Value("${app.upload.dir}")
    private String uploadDir;

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

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<LivreDTO> updateStock(
            @PathVariable Long id,
            @RequestParam Integer stock,
            @RequestParam(required = false) Integer seuilAlerte) {
        return ResponseEntity.ok(livreService.updateStock(id, stock, seuilAlerte));
    }

    @GetMapping("/admin/stock-bas")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<List<LivreDTO>> findStockBas() {
        return ResponseEntity.ok(livreService.findStockBas());
    }

    @GetMapping("/admin/rupture-stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<List<LivreDTO>> findRuptureStock() {
        return ResponseEntity.ok(livreService.findRuptureStock());
    }

    @GetMapping("/admin/stock-stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<?> getStockStats() {
        return ResponseEntity.ok(livreService.getStockStats());
    }

    @PostMapping("/{id}/pdf")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<LivreDTO> uploadPdf(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Create pdfs subdirectory
            Path pdfDir = Paths.get(uploadDir, "pdfs");
            Files.createDirectories(pdfDir);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".pdf";
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path targetPath = pdfDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Update book with PDF path
            return ResponseEntity.ok(livreService.updatePdfPath(id, "pdfs/" + filename));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}/pdf")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<LivreDTO> deletePdf(@PathVariable Long id) {
        LivreDTO livre = livreService.findById(id);

        if (livre.getPdfPath() != null && !livre.getPdfPath().isEmpty()) {
            try {
                Path filePath = Paths.get(uploadDir).resolve(livre.getPdfPath()).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log error but continue to clear the path
            }
        }

        return ResponseEntity.ok(livreService.updatePdfPath(id, null));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> getPdf(@PathVariable Long id) {
        LivreDTO livre = livreService.findById(id);

        if (livre.getPdfPath() == null || livre.getPdfPath().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get(uploadDir).resolve(livre.getPdfPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + livre.getTitre() + ".pdf\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
