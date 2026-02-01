package com.maisonedition.controller;

import com.maisonedition.dto.ProfilDTO;
import com.maisonedition.entity.Utilisateur;
import com.maisonedition.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profil")
@RequiredArgsConstructor
public class ProfilController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<?> getProfil() {
        Utilisateur user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "nom", user.getNom(),
            "prenom", user.getPrenom(),
            "role", user.getRole().name(),
            "telephone", user.getTelephone() != null ? user.getTelephone() : "",
            "adresse", user.getAdresse() != null ? user.getAdresse() : "",
            "ville", user.getVille() != null ? user.getVille() : "",
            "codePostal", user.getCodePostal() != null ? user.getCodePostal() : "",
            "pays", user.getPays() != null ? user.getPays() : ""
        ));
    }

    @PutMapping
    public ResponseEntity<?> updateProfil(@Valid @RequestBody ProfilDTO.UpdateProfilRequest request) {
        Utilisateur user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        utilisateurRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Profil mis à jour",
            "nom", user.getNom(),
            "prenom", user.getPrenom()
        ));
    }

    @PutMapping("/mot-de-passe")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ProfilDTO.ChangePasswordRequest request) {
        Utilisateur user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        if (!passwordEncoder.matches(request.getMotDePasseActuel(), user.getMotDePasse())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mot de passe actuel incorrect"));
        }

        user.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateurRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
    }

    @PutMapping("/adresse")
    public ResponseEntity<?> updateAdresse(@RequestBody AdresseRequest request) {
        Utilisateur user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        user.setTelephone(request.telephone);
        user.setAdresse(request.adresse);
        user.setVille(request.ville);
        user.setCodePostal(request.codePostal);
        user.setPays(request.pays);

        utilisateurRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Adresse mise à jour",
            "telephone", user.getTelephone() != null ? user.getTelephone() : "",
            "adresse", user.getAdresse() != null ? user.getAdresse() : "",
            "ville", user.getVille() != null ? user.getVille() : "",
            "codePostal", user.getCodePostal() != null ? user.getCodePostal() : "",
            "pays", user.getPays() != null ? user.getPays() : ""
        ));
    }

    private Utilisateur getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        String email = authentication.getName();
        return utilisateurRepository.findByEmail(email).orElse(null);
    }

    public static class AdresseRequest {
        public String telephone;
        public String adresse;
        public String ville;
        public String codePostal;
        public String pays;
    }
}
