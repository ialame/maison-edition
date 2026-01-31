package com.maisonedition.controller;

import com.maisonedition.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<Map<String, String>> uploadFile(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file) {

        // Valider le type
        if (!type.matches("^(livres|auteurs|articles|evenements)$")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Type invalide"));
        }

        String filePath = fileStorageService.storeFile(file, type);
        return ResponseEntity.ok(Map.of(
                "path", filePath,
                "url", "/uploads/" + filePath
        ));
    }

    @DeleteMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<Void> deleteFile(@RequestParam String path) {
        fileStorageService.deleteFile(path);
        return ResponseEntity.ok().build();
    }
}
