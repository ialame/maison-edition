package com.maisonedition.service;

import com.maisonedition.dto.AuthDTO;
import com.maisonedition.entity.Utilisateur;
import com.maisonedition.repository.UtilisateurRepository;
import com.maisonedition.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request) {
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un compte avec cet email existe déjà");
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .email(request.getEmail())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .role(Utilisateur.Role.UTILISATEUR)
                .actif(true)
                .build();

        utilisateurRepository.save(utilisateur);

        UserDetails userDetails = new User(
                utilisateur.getEmail(),
                utilisateur.getMotDePasse(),
                Collections.emptyList()
        );

        String token = jwtService.generateToken(userDetails);

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .email(utilisateur.getEmail())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .role(utilisateur.getRole().name())
                .build();
    }

    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getMotDePasse()
                )
        );

        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        UserDetails userDetails = new User(
                utilisateur.getEmail(),
                utilisateur.getMotDePasse(),
                Collections.emptyList()
        );

        String token = jwtService.generateToken(userDetails);

        return AuthDTO.AuthResponse.builder()
                .token(token)
                .email(utilisateur.getEmail())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .role(utilisateur.getRole().name())
                .build();
    }
}
