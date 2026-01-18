package com.maisonedition.config;

import com.maisonedition.entity.Utilisateur;
import com.maisonedition.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Créer un compte admin par défaut s'il n'existe pas
        if (!utilisateurRepository.existsByEmail("admin@daralnashr.com")) {
            Utilisateur admin = Utilisateur.builder()
                    .email("admin@daralnashr.com")
                    .motDePasse(passwordEncoder.encode("admin123"))
                    .nom("المدير")
                    .prenom("النظام")
                    .role(Utilisateur.Role.ADMIN)
                    .actif(true)
                    .build();

            utilisateurRepository.save(admin);
            log.info("تم إنشاء حساب المدير الافتراضي: admin@daralnashr.com / admin123");
        }

        // Créer un compte éditeur par défaut s'il n'existe pas
        if (!utilisateurRepository.existsByEmail("editeur@daralnashr.com")) {
            Utilisateur editeur = Utilisateur.builder()
                    .email("editeur@daralnashr.com")
                    .motDePasse(passwordEncoder.encode("editeur123"))
                    .nom("المحرر")
                    .prenom("الأول")
                    .role(Utilisateur.Role.EDITEUR)
                    .actif(true)
                    .build();

            utilisateurRepository.save(editeur);
            log.info("تم إنشاء حساب المحرر الافتراضي: editeur@daralnashr.com / editeur123");
        }
    }
}
