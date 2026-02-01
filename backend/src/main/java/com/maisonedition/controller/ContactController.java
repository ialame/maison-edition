package com.maisonedition.controller;

import com.maisonedition.entity.Contact;
import com.maisonedition.repository.ContactRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactRepository contactRepository;

    // Public endpoint - anyone can submit a contact message
    @PostMapping
    public ResponseEntity<?> submitContact(@Valid @RequestBody ContactRequest request) {
        Contact contact = Contact.builder()
                .nom(request.getNom())
                .email(request.getEmail())
                .sujet(request.getSujet())
                .message(request.getMessage())
                .build();

        contactRepository.save(contact);

        return ResponseEntity.ok(Map.of("message", "Message envoyé avec succès"));
    }

    // Admin endpoints
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Contact>> getAllContacts() {
        return ResponseEntity.ok(contactRepository.findAllByOrderByDateCreationDesc());
    }

    @GetMapping("/admin/non-lus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", contactRepository.countByLuFalse()));
    }

    @PutMapping("/admin/{id}/lu")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        return contactRepository.findById(id)
                .map(contact -> {
                    if (!contact.getLu()) {
                        contact.setLu(true);
                        contact.setDateLecture(LocalDateTime.now());
                        contactRepository.save(contact);
                    }
                    return ResponseEntity.ok(Map.of("message", "Marqué comme lu"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/admin/{id}/traite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleTraite(@PathVariable Long id) {
        return contactRepository.findById(id)
                .map(contact -> {
                    contact.setTraite(!contact.getTraite());
                    contactRepository.save(contact);
                    return ResponseEntity.ok(Map.of(
                            "message", contact.getTraite() ? "Marqué comme traité" : "Marqué comme non traité",
                            "traite", contact.getTraite()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/admin/{id}/notes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateNotes(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String notes = body.get("notes");
        return contactRepository.findById(id)
                .map(contact -> {
                    contact.setNotes(notes);
                    contactRepository.save(contact);
                    return ResponseEntity.ok(Map.of("message", "Notes mises à jour"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteContact(@PathVariable Long id) {
        if (!contactRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        contactRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Message supprimé"));
    }

    @Data
    public static class ContactRequest {
        @NotBlank(message = "Le nom est obligatoire")
        private String nom;

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Email invalide")
        private String email;

        @NotBlank(message = "Le sujet est obligatoire")
        private String sujet;

        @NotBlank(message = "Le message est obligatoire")
        private String message;
    }
}
