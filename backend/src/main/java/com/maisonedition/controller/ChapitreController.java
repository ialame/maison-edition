package com.maisonedition.controller;

import com.maisonedition.dto.ChapitreDTO;
import com.maisonedition.dto.ChapitreDetailDTO;
import com.maisonedition.dto.ChapitreListDTO;
import com.maisonedition.entity.Chapitre;
import com.maisonedition.entity.Utilisateur;
import com.maisonedition.repository.UtilisateurRepository;
import com.maisonedition.service.AccessService;
import com.maisonedition.service.ChapitreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChapitreController {

    private final ChapitreService chapitreService;
    private final AccessService accessService;
    private final UtilisateurRepository utilisateurRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    // ===== PUBLIC ENDPOINTS =====

    @GetMapping("/livres/{livreId}/chapitres")
    public ResponseEntity<List<ChapitreListDTO>> getChapitresByLivre(@PathVariable Long livreId) {
        List<Chapitre> chapitres = chapitreService.findPubliesByLivreId(livreId);
        List<ChapitreListDTO> dtos = chapitres.stream()
                .map(this::toListDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/livres/{livreId}/chapitres/gratuits")
    public ResponseEntity<List<ChapitreListDTO>> getChapitresGratuits(@PathVariable Long livreId) {
        List<Chapitre> chapitres = chapitreService.findGratuitsByLivreId(livreId);
        List<ChapitreListDTO> dtos = chapitres.stream()
                .map(this::toListDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Vérifie si l'utilisateur connecté a accès au livre (achat ou abonnement actif)
     */
    @GetMapping("/livres/{livreId}/acces")
    public ResponseEntity<java.util.Map<String, Boolean>> checkAccess(@PathVariable Long livreId) {
        Long utilisateurId = getCurrentUserId();
        boolean hasAccess = utilisateurId != null && accessService.hasAccessToBook(utilisateurId, livreId);
        return ResponseEntity.ok(java.util.Map.of("hasAccess", hasAccess));
    }

    @GetMapping("/livres/{livreId}/chapitres/{numero}")
    public ResponseEntity<ChapitreDetailDTO> getChapitre(
            @PathVariable Long livreId,
            @PathVariable Integer numero) {
        Chapitre chapitre = chapitreService.findByLivreIdAndNumero(livreId, numero);

        // Si le chapitre est gratuit, tout le monde peut le lire
        if (chapitre.getGratuit()) {
            return ResponseEntity.ok(toDetailDTO(chapitre));
        }

        // Sinon, vérifier si l'utilisateur a accès (achat ou abonnement)
        Long utilisateurId = getCurrentUserId();
        if (utilisateurId != null && accessService.hasAccessToBook(utilisateurId, livreId)) {
            return ResponseEntity.ok(toDetailDTO(chapitre));
        }

        // Pas d'accès
        return ResponseEntity.status(403).build();
    }

    @GetMapping("/livres/{livreId}/chapitres/{numero}/pdf")
    public ResponseEntity<Resource> getChapitrePdf(
            @PathVariable Long livreId,
            @PathVariable Integer numero) {
        Chapitre chapitre = chapitreService.findByLivreIdAndNumero(livreId, numero);

        // Vérifier l'accès : gratuit ou achat/abonnement
        boolean hasAccess = chapitre.getGratuit();
        if (!hasAccess) {
            Long utilisateurId = getCurrentUserId();
            hasAccess = utilisateurId != null && accessService.hasAccessToBook(utilisateurId, livreId);
        }

        if (!hasAccess) {
            return ResponseEntity.status(403).build();
        }

        if (chapitre.getPdfPath() == null || chapitre.getPdfPath().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get(uploadDir).resolve(chapitre.getPdfPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + chapitre.getTitre() + ".pdf\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== ADMIN ENDPOINTS =====

    @GetMapping("/admin/livres/{livreId}/chapitres")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<List<ChapitreDTO>> getChapitresAdmin(@PathVariable Long livreId) {
        List<Chapitre> chapitres = chapitreService.findByLivreId(livreId);
        List<ChapitreDTO> dtos = chapitres.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/admin/chapitres/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<ChapitreDTO> getChapitreAdmin(@PathVariable Long id) {
        Chapitre chapitre = chapitreService.findById(id);
        return ResponseEntity.ok(toDTO(chapitre));
    }

    @PostMapping("/admin/livres/{livreId}/chapitres")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<ChapitreDTO> createChapitre(
            @PathVariable Long livreId,
            @RequestBody ChapitreDTO dto) {
        Chapitre chapitre = toEntity(dto);
        Chapitre saved = chapitreService.create(livreId, chapitre);
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/admin/chapitres/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<ChapitreDTO> updateChapitre(
            @PathVariable Long id,
            @RequestBody ChapitreDTO dto) {
        Chapitre chapitre = toEntity(dto);
        Chapitre updated = chapitreService.update(id, chapitre);
        return ResponseEntity.ok(toDTO(updated));
    }

    @PostMapping("/admin/chapitres/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<ChapitreDTO> uploadChapitrePdf(
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

            // Update chapter with PDF path
            Chapitre updated = chapitreService.updatePdfPath(id, "pdfs/" + filename);
            return ResponseEntity.ok(toDTO(updated));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/admin/chapitres/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<ChapitreDTO> deleteChapitrePdf(@PathVariable Long id) {
        Chapitre chapitre = chapitreService.findById(id);

        if (chapitre.getPdfPath() != null && !chapitre.getPdfPath().isEmpty()) {
            try {
                Path filePath = Paths.get(uploadDir).resolve(chapitre.getPdfPath()).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log error but continue to clear the path
            }
        }

        Chapitre updated = chapitreService.updatePdfPath(id, null);
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/admin/chapitres/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<Void> deleteChapitre(@PathVariable Long id) {
        chapitreService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/livres/{livreId}/chapitres/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<Void> reorderChapitres(
            @PathVariable Long livreId,
            @RequestBody List<Long> chapitreIds) {
        chapitreService.reorder(livreId, chapitreIds);
        return ResponseEntity.ok().build();
    }

    // ===== DTO CONVERTERS =====

    private ChapitreDTO toDTO(Chapitre chapitre) {
        return ChapitreDTO.builder()
                .id(chapitre.getId())
                .titre(chapitre.getTitre())
                .contenu(chapitre.getContenu())
                .numero(chapitre.getNumero())
                .gratuit(chapitre.getGratuit())
                .publie(chapitre.getPublie())
                .pdfPath(chapitre.getPdfPath())
                .livreId(chapitre.getLivre().getId())
                .dateCreation(chapitre.getDateCreation())
                .dateModification(chapitre.getDateModification())
                .build();
    }

    private ChapitreListDTO toListDTO(Chapitre chapitre) {
        return ChapitreListDTO.builder()
                .id(chapitre.getId())
                .titre(chapitre.getTitre())
                .numero(chapitre.getNumero())
                .gratuit(chapitre.getGratuit())
                .build();
    }

    private ChapitreDetailDTO toDetailDTO(Chapitre chapitre) {
        return ChapitreDetailDTO.builder()
                .id(chapitre.getId())
                .titre(chapitre.getTitre())
                .contenu(chapitre.getContenu())
                .numero(chapitre.getNumero())
                .gratuit(chapitre.getGratuit())
                .pdfPath(chapitre.getPdfPath())
                .livreId(chapitre.getLivre().getId())
                .livreTitre(chapitre.getLivre().getTitre())
                .build();
    }

    private Chapitre toEntity(ChapitreDTO dto) {
        return Chapitre.builder()
                .titre(dto.getTitre())
                .contenu(dto.getContenu())
                .numero(dto.getNumero())
                .gratuit(dto.getGratuit() != null ? dto.getGratuit() : false)
                .publie(dto.getPublie() != null ? dto.getPublie() : true)
                .build();
    }

    /**
     * Récupère l'ID de l'utilisateur connecté, ou null si non connecté
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        String email = authentication.getName();
        return utilisateurRepository.findByEmail(email)
                .map(Utilisateur::getId)
                .orElse(null);
    }
}
