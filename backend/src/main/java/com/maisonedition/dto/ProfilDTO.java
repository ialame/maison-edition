package com.maisonedition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class ProfilDTO {

    @Data
    public static class UpdateProfilRequest {
        @NotBlank(message = "Le nom est obligatoire")
        private String nom;

        @NotBlank(message = "Le prénom est obligatoire")
        private String prenom;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Le mot de passe actuel est obligatoire")
        private String motDePasseActuel;

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        private String nouveauMotDePasse;
    }
}
