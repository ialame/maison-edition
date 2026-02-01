package com.maisonedition.service;

import com.maisonedition.repository.CommandeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Service pour vérifier les droits d'accès aux contenus payants
 */
@Service
@RequiredArgsConstructor
public class AccessService {

    private final CommandeRepository commandeRepository;

    /**
     * Vérifie si un utilisateur a accès au contenu d'un livre
     *
     * @param utilisateurId ID de l'utilisateur (peut être null si non connecté)
     * @param livreId ID du livre
     * @return true si l'utilisateur a accès (achat, abonnement livre ou abonnement global)
     */
    public boolean hasAccessToBook(Long utilisateurId, Long livreId) {
        if (utilisateurId == null) {
            return false;
        }

        LocalDate today = LocalDate.now();

        // Vérifier si l'utilisateur a acheté le livre (papier ou numérique)
        if (commandeRepository.hasAchatAccess(utilisateurId, livreId)) {
            return true;
        }

        // Vérifier si l'utilisateur a un abonnement LECTURE_LIVRE actif pour ce livre
        if (commandeRepository.hasLectureLivreActif(utilisateurId, livreId, today)) {
            return true;
        }

        // Vérifier si l'utilisateur a un abonnement global actif (tous les livres)
        return commandeRepository.hasAbonnementGlobalActif(utilisateurId, today);
    }
}
