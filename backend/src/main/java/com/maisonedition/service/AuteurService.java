package com.maisonedition.service;

import com.maisonedition.dto.AuteurDTO;
import com.maisonedition.entity.Auteur;
import com.maisonedition.repository.AuteurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AuteurService {

    private final AuteurRepository auteurRepository;

    public List<AuteurDTO> findAll() {
        return auteurRepository.findAll().stream()
                .map(AuteurDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public AuteurDTO findById(Long id) {
        return auteurRepository.findById(id)
                .map(AuteurDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Auteur non trouvé avec l'id: " + id));
    }

    public List<AuteurDTO> findAuteursAvecLivres() {
        return auteurRepository.findAuteursAvecLivres().stream()
                .map(AuteurDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuteurDTO> rechercher(String terme) {
        return auteurRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(terme, terme).stream()
                .map(AuteurDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public AuteurDTO create(AuteurDTO dto) {
        Auteur auteur = Auteur.builder()
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .biographie(dto.getBiographie())
                .photo(dto.getPhoto())
                .dateNaissance(dto.getDateNaissance())
                .nationalite(dto.getNationalite())
                .siteWeb(dto.getSiteWeb())
                .build();

        return AuteurDTO.fromEntity(auteurRepository.save(auteur));
    }

    public AuteurDTO update(Long id, AuteurDTO dto) {
        Auteur auteur = auteurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auteur non trouvé avec l'id: " + id));

        auteur.setNom(dto.getNom());
        auteur.setPrenom(dto.getPrenom());
        auteur.setBiographie(dto.getBiographie());
        auteur.setPhoto(dto.getPhoto());
        auteur.setDateNaissance(dto.getDateNaissance());
        auteur.setNationalite(dto.getNationalite());
        auteur.setSiteWeb(dto.getSiteWeb());

        return AuteurDTO.fromEntity(auteurRepository.save(auteur));
    }

    public void delete(Long id) {
        auteurRepository.deleteById(id);
    }
}
