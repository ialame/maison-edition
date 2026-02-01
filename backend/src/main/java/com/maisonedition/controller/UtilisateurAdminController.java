package com.maisonedition.controller;

import com.maisonedition.entity.Utilisateur;
import com.maisonedition.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UtilisateurAdminController {

    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public ResponseEntity<List<UtilisateurDTO>> getAllUtilisateurs() {
        List<UtilisateurDTO> utilisateurs = utilisateurRepository.findAll().stream()
                .map(UtilisateurDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(utilisateurs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurDTO> getUtilisateur(@PathVariable Long id) {
        return utilisateurRepository.findById(id)
                .map(UtilisateurDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String roleStr = body.get("role");
        if (roleStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role requis"));
        }

        try {
            Utilisateur.Role role = Utilisateur.Role.valueOf(roleStr);
            return utilisateurRepository.findById(id)
                    .map(user -> {
                        user.setRole(role);
                        utilisateurRepository.save(user);
                        return ResponseEntity.ok(Map.of("message", "Rôle mis à jour", "role", role.name()));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rôle invalide"));
        }
    }

    @PutMapping("/{id}/toggle-actif")
    public ResponseEntity<?> toggleActif(@PathVariable Long id) {
        return utilisateurRepository.findById(id)
                .map(user -> {
                    user.setActif(!user.getActif());
                    utilisateurRepository.save(user);
                    return ResponseEntity.ok(Map.of(
                            "message", user.getActif() ? "Utilisateur activé" : "Utilisateur désactivé",
                            "actif", user.getActif()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record UtilisateurDTO(
            Long id,
            String email,
            String nom,
            String prenom,
            String role,
            Boolean actif,
            String telephone,
            String adresse,
            String ville,
            String codePostal,
            String pays,
            String dateCreation
    ) {
        public static UtilisateurDTO fromEntity(Utilisateur u) {
            return new UtilisateurDTO(
                    u.getId(),
                    u.getEmail(),
                    u.getNom(),
                    u.getPrenom(),
                    u.getRole().name(),
                    u.getActif(),
                    u.getTelephone(),
                    u.getAdresse(),
                    u.getVille(),
                    u.getCodePostal(),
                    u.getPays(),
                    u.getDateCreation() != null ? u.getDateCreation().toString() : null
            );
        }
    }
}
