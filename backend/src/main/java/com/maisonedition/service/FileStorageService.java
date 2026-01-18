package com.maisonedition.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            // Créer les sous-dossiers
            Files.createDirectories(uploadPath.resolve("livres"));
            Files.createDirectories(uploadPath.resolve("auteurs"));
            Files.createDirectories(uploadPath.resolve("articles"));
            Files.createDirectories(uploadPath.resolve("evenements"));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier d'upload", e);
        }
    }

    public String storeFile(MultipartFile file, String subFolder) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Le fichier est vide");
            }

            // Valider le type de fichier
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("Seules les images sont autorisées");
            }

            // Générer un nom unique
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // Sauvegarder le fichier
            Path targetPath = uploadPath.resolve(subFolder).resolve(newFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return subFolder + "/" + newFilename;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier", e);
        }
    }

    public void deleteFile(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            try {
                Path path = uploadPath.resolve(filePath);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                // Log l'erreur mais ne pas bloquer
            }
        }
    }

    public Path getFilePath(String filePath) {
        return uploadPath.resolve(filePath);
    }
}
