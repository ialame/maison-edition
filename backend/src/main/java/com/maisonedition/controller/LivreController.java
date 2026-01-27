package com.maisonedition.controller;

import com.maisonedition.dto.LivreDTO;
import com.maisonedition.entity.Livre;
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    // ===== EPUB ENDPOINTS =====

    @GetMapping("/{id}/epub")
    public ResponseEntity<Resource> getEpub(@PathVariable Long id) {
        Livre livre = livreService.findEntityById(id);

        if (livre.getEpubPath() == null || livre.getEpubPath().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get(uploadDir).resolve(livre.getEpubPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/epub+zip"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + livre.getTitre() + ".epub\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Serve individual files from inside the EPUB (ZIP) for lazy loading
    private static final Map<String, String> EPUB_MIME_TYPES = Map.ofEntries(
            Map.entry("xml", "application/xml"),
            Map.entry("xhtml", "application/xhtml+xml"),
            Map.entry("html", "text/html"),
            Map.entry("htm", "text/html"),
            Map.entry("css", "text/css"),
            Map.entry("js", "application/javascript"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("ttf", "font/ttf"),
            Map.entry("otf", "font/otf"),
            Map.entry("woff", "font/woff"),
            Map.entry("woff2", "font/woff2"),
            Map.entry("ncx", "application/x-dtbncx+xml"),
            Map.entry("opf", "application/oebps-package+xml"),
            Map.entry("smil", "application/smil+xml")
    );

    @GetMapping("/{id}/epub/{*entryPath}")
    public ResponseEntity<byte[]> getEpubEntry(@PathVariable Long id, @PathVariable String entryPath) {
        Livre livre = livreService.findEntityById(id);

        if (livre.getEpubPath() == null || livre.getEpubPath().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(uploadDir).resolve(livre.getEpubPath()).normalize();
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        try (ZipFile zipFile = new ZipFile(filePath.toFile())) {
            // Remove leading slash if present
            String cleanPath = entryPath.startsWith("/") ? entryPath.substring(1) : entryPath;

            ZipEntry entry = zipFile.getEntry(cleanPath);
            if (entry == null) {
                return ResponseEntity.notFound().build();
            }

            try (InputStream is = zipFile.getInputStream(entry)) {
                byte[] content = is.readAllBytes();

                String ext = cleanPath.contains(".")
                        ? cleanPath.substring(cleanPath.lastIndexOf('.') + 1).toLowerCase()
                        : "";
                String contentType = EPUB_MIME_TYPES.getOrDefault(ext, "application/octet-stream");

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                        .body(content);
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/epub")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<LivreDTO> uploadEpub(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path epubDir = Paths.get(uploadDir, "epubs");
            Files.createDirectories(epubDir);

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".epub";
            String filename = UUID.randomUUID().toString() + extension;

            Path targetPath = epubDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            Livre updated = livreService.updateEpubPath(id, "epubs/" + filename);
            return ResponseEntity.ok(LivreDTO.fromEntity(updated));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}/epub")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITEUR')")
    public ResponseEntity<LivreDTO> deleteEpub(@PathVariable Long id) {
        Livre livre = livreService.findEntityById(id);

        if (livre.getEpubPath() != null && !livre.getEpubPath().isEmpty()) {
            try {
                Path filePath = Paths.get(uploadDir).resolve(livre.getEpubPath()).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log but continue
            }
        }

        Livre updated = livreService.updateEpubPath(id, null);
        return ResponseEntity.ok(LivreDTO.fromEntity(updated));
    }
}
