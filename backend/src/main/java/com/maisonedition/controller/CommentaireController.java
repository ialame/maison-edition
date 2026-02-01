package com.maisonedition.controller;

import com.maisonedition.dto.CommentaireDTO;
import com.maisonedition.entity.Article;
import com.maisonedition.entity.Commentaire;
import com.maisonedition.entity.Utilisateur;
import com.maisonedition.repository.ArticleRepository;
import com.maisonedition.repository.CommentaireRepository;
import com.maisonedition.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/commentaires")
@RequiredArgsConstructor
public class CommentaireController {

    private final CommentaireRepository commentaireRepository;
    private final ArticleRepository articleRepository;
    private final UtilisateurRepository utilisateurRepository;

    // Public: Get approved comments for an article
    @GetMapping("/article/{articleId}")
    public ResponseEntity<List<CommentaireDTO>> getByArticle(@PathVariable Long articleId) {
        List<CommentaireDTO> commentaires = commentaireRepository.findByArticleIdAndApprouveTrue(articleId)
                .stream()
                .map(CommentaireDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commentaires);
    }

    // Public: Add a comment
    @PostMapping("/article/{articleId}")
    public ResponseEntity<?> addComment(
            @PathVariable Long articleId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String contenu = request.get("contenu");
        String nomAuteur = request.get("nomAuteur");

        if (contenu == null || contenu.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "التعليق مطلوب"));
        }

        Article article = articleRepository.findById(articleId)
                .orElse(null);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }

        Commentaire.CommentaireBuilder builder = Commentaire.builder()
                .contenu(contenu.trim())
                .article(article);

        // If authenticated, use user info and auto-approve
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            String email = authentication.getName();
            Utilisateur user = utilisateurRepository.findByEmail(email).orElse(null);
            if (user != null) {
                builder.utilisateur(user)
                       .nomAuteur(user.getPrenom() + " " + user.getNom())
                       .emailAuteur(user.getEmail())
                       .approuve(true); // Auto-approve for authenticated users
            }
        } else {
            // Anonymous comment
            if (nomAuteur == null || nomAuteur.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "الاسم مطلوب"));
            }
            builder.nomAuteur(nomAuteur.trim())
                   .emailAuteur(request.get("email"))
                   .approuve(false); // Require moderation for anonymous
        }

        Commentaire saved = commentaireRepository.save(builder.build());
        return ResponseEntity.ok(Map.of(
                "message", saved.getApprouve()
                        ? "تم نشر تعليقك بنجاح"
                        : "شكراً لتعليقك! سيتم نشره بعد المراجعة",
                "commentaire", CommentaireDTO.fromEntity(saved)
        ));
    }

    // Admin: Get all comments
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<List<CommentaireDTO>> getAll() {
        return ResponseEntity.ok(commentaireRepository.findAll().stream()
                .map(CommentaireDTO::fromEntity)
                .collect(Collectors.toList()));
    }

    // Admin: Get pending comments
    @GetMapping("/admin/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<List<CommentaireDTO>> getPending() {
        return ResponseEntity.ok(commentaireRepository.findByApprouveFalse().stream()
                .map(CommentaireDTO::fromEntity)
                .collect(Collectors.toList()));
    }

    // Admin: Approve a comment
    @PutMapping("/admin/{id}/approuver")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        return commentaireRepository.findById(id)
                .map(c -> {
                    c.setApprouve(true);
                    commentaireRepository.save(c);
                    return ResponseEntity.ok(Map.of("message", "تمت الموافقة على التعليق"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Admin: Delete a comment
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (commentaireRepository.existsById(id)) {
            commentaireRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "تم حذف التعليق"));
        }
        return ResponseEntity.notFound().build();
    }

    // Admin: Get stats
    @GetMapping("/admin/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(Map.of(
                "total", commentaireRepository.count(),
                "enAttente", commentaireRepository.countByApprouveFalse()
        ));
    }
}
