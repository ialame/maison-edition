package com.maisonedition.controller;

import com.maisonedition.entity.Chapitre;
import com.maisonedition.entity.Utilisateur;
import com.maisonedition.repository.UtilisateurRepository;
import com.maisonedition.service.AccessService;
import com.maisonedition.service.ChapitreService;
import com.maisonedition.service.TTSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TTSController {

    private final TTSService ttsService;
    private final ChapitreService chapitreService;
    private final AccessService accessService;
    private final UtilisateurRepository utilisateurRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * Check if audio is available (cached) for a chapter
     */
    @GetMapping("/livres/{livreId}/chapitres/{numero}/audio/status")
    public ResponseEntity<Map<String, Object>> getAudioStatus(
            @PathVariable Long livreId,
            @PathVariable Integer numero) {

        // Check access
        if (!hasAccess(livreId, numero)) {
            return ResponseEntity.status(403).build();
        }

        boolean cached = ttsService.hasAudioCache(livreId, numero);
        return ResponseEntity.ok(Map.of(
            "available", true,
            "cached", cached
        ));
    }

    /**
     * Get or generate audio for a chapter
     * Returns the MP3 file
     */
    @GetMapping("/livres/{livreId}/chapitres/{numero}/audio")
    public ResponseEntity<Resource> getAudio(
            @PathVariable Long livreId,
            @PathVariable Integer numero) {

        // Check access
        if (!hasAccess(livreId, numero)) {
            return ResponseEntity.status(403).build();
        }

        try {
            String relativePath = ttsService.getOrGenerateAudio(livreId, numero);
            Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                Chapitre chapitre = chapitreService.findByLivreIdAndNumero(livreId, numero);
                String filename = chapitre.getTitre().replaceAll("[^\\p{L}\\p{N}\\s]", "") + ".mp3";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("audio/mpeg"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000") // Cache for 1 year
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Invalid audio file path", e);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Failed to generate audio", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if user has access to the chapter (free chapter or purchased/subscribed)
     */
    private boolean hasAccess(Long livreId, Integer numero) {
        try {
            Chapitre chapitre = chapitreService.findByLivreIdAndNumero(livreId, numero);

            // Free chapters are accessible to everyone
            if (chapitre.getGratuit()) {
                return true;
            }

            // Check if user has purchased or has active subscription
            Long utilisateurId = getCurrentUserId();
            return utilisateurId != null && accessService.hasAccessToBook(utilisateurId, livreId);
        } catch (Exception e) {
            return false;
        }
    }

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
