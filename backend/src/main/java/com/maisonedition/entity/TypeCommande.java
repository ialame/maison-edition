package com.maisonedition.entity;

public enum TypeCommande {
    PAPIER,              // Livre papier (prix variable)
    NUMERIQUE,           // Téléchargement PDF (10€)
    LECTURE_LIVRE,       // Lecture d'un livre pendant 1 an (5€)
    ABONNEMENT_MENSUEL,  // Tous les livres pendant 1 mois (30€)
    ABONNEMENT_ANNUEL    // Tous les livres pendant 1 an (50€)
}
