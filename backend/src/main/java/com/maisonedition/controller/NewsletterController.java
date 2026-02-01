package com.maisonedition.controller;

import com.maisonedition.entity.NewsletterAbonne;
import com.maisonedition.repository.NewsletterAbonneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterAbonneRepository newsletterRepository;

    // Public endpoint - subscribe
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("message", "البريد الإلكتروني غير صالح"));
        }

        email = email.trim().toLowerCase();

        // Check if already subscribed
        var existing = newsletterRepository.findByEmail(email);
        if (existing.isPresent()) {
            NewsletterAbonne abonne = existing.get();
            if (abonne.getActif()) {
                return ResponseEntity.ok(Map.of("message", "أنت مشترك بالفعل في النشرة البريدية"));
            } else {
                // Reactivate
                abonne.setActif(true);
                abonne.setDateDesinscription(null);
                newsletterRepository.save(abonne);
                return ResponseEntity.ok(Map.of("message", "تم إعادة تفعيل اشتراكك في النشرة البريدية"));
            }
        }

        NewsletterAbonne abonne = NewsletterAbonne.builder()
                .email(email)
                .build();
        newsletterRepository.save(abonne);

        return ResponseEntity.ok(Map.of("message", "شكراً لاشتراكك في نشرتنا البريدية!"));
    }

    // Public endpoint - unsubscribe
    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "البريد الإلكتروني مطلوب"));
        }

        var existing = newsletterRepository.findByEmail(email.trim().toLowerCase());
        if (existing.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "البريد الإلكتروني غير مسجل"));
        }

        NewsletterAbonne abonne = existing.get();
        abonne.setActif(false);
        abonne.setDateDesinscription(LocalDateTime.now());
        newsletterRepository.save(abonne);

        return ResponseEntity.ok(Map.of("message", "تم إلغاء اشتراكك بنجاح"));
    }

    // Admin endpoints
    @GetMapping("/admin/abonnes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NewsletterAbonne>> getAllAbonnes() {
        return ResponseEntity.ok(newsletterRepository.findAll());
    }

    @GetMapping("/admin/abonnes/actifs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NewsletterAbonne>> getActifAbonnes() {
        return ResponseEntity.ok(newsletterRepository.findByActifTrue());
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(Map.of(
                "total", newsletterRepository.count(),
                "actifs", newsletterRepository.countByActifTrue()
        ));
    }

    @DeleteMapping("/admin/abonnes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAbonne(@PathVariable Long id) {
        if (newsletterRepository.existsById(id)) {
            newsletterRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "تم الحذف بنجاح"));
        }
        return ResponseEntity.notFound().build();
    }
}
