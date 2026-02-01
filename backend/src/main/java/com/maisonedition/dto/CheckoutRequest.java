package com.maisonedition.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private Long livreId;
    private String type; // PAPIER, NUMERIQUE, LECTURE_LIVRE, ABONNEMENT_MENSUEL, ABONNEMENT_ANNUEL

    // Shipping info (for paper orders)
    private String nomComplet;
    private String adresse;
    private String ville;
    private String codePostal;
    private String pays;
    private String telephone;
}
