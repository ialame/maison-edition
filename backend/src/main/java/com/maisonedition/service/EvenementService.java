package com.maisonedition.service;

import com.maisonedition.dto.EvenementDTO;
import com.maisonedition.entity.Auteur;
import com.maisonedition.entity.Evenement;
import com.maisonedition.entity.Livre;
import com.maisonedition.repository.AuteurRepository;
import com.maisonedition.repository.EvenementRepository;
import com.maisonedition.repository.LivreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EvenementService {

    private final EvenementRepository evenementRepository;
    private final LivreRepository livreRepository;
    private final AuteurRepository auteurRepository;

    public List<EvenementDTO> findAll() {
        return evenementRepository.findAll().stream()
                .map(EvenementDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public EvenementDTO findById(Long id) {
        return evenementRepository.findById(id)
                .map(EvenementDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé avec l'id: " + id));
    }

    public List<EvenementDTO> findAVenir(int limit) {
        return evenementRepository.findEvenementsAVenir(LocalDateTime.now(), PageRequest.of(0, limit)).stream()
                .map(EvenementDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<EvenementDTO> findPasses(Pageable pageable) {
        return evenementRepository.findEvenementsPasses(LocalDateTime.now(), pageable)
                .map(EvenementDTO::fromEntity);
    }

    public List<EvenementDTO> findByType(Evenement.TypeEvenement type) {
        return evenementRepository.findByTypeAndActifTrue(type).stream()
                .map(EvenementDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public EvenementDTO create(EvenementDTO dto, Long livreId, Long auteurId) {
        Evenement evenement = Evenement.builder()
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .image(dto.getImage())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .lieu(dto.getLieu())
                .adresse(dto.getAdresse())
                .ville(dto.getVille())
                .type(dto.getType() != null ? Evenement.TypeEvenement.valueOf(dto.getType()) : Evenement.TypeEvenement.AUTRE)
                .actif(dto.getActif() != null ? dto.getActif() : true)
                .build();

        if (livreId != null) {
            Livre livre = livreRepository.findById(livreId)
                    .orElseThrow(() -> new RuntimeException("Livre non trouvé"));
            evenement.setLivre(livre);
        }

        if (auteurId != null) {
            Auteur auteur = auteurRepository.findById(auteurId)
                    .orElseThrow(() -> new RuntimeException("Auteur non trouvé"));
            evenement.setAuteur(auteur);
        }

        return EvenementDTO.fromEntity(evenementRepository.save(evenement));
    }

    public EvenementDTO update(Long id, EvenementDTO dto, Long livreId, Long auteurId) {
        Evenement evenement = evenementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé avec l'id: " + id));

        evenement.setTitre(dto.getTitre());
        evenement.setDescription(dto.getDescription());
        evenement.setImage(dto.getImage());
        evenement.setDateDebut(dto.getDateDebut());
        evenement.setDateFin(dto.getDateFin());
        evenement.setLieu(dto.getLieu());
        evenement.setAdresse(dto.getAdresse());
        evenement.setVille(dto.getVille());
        evenement.setType(dto.getType() != null ? Evenement.TypeEvenement.valueOf(dto.getType()) : evenement.getType());
        evenement.setActif(dto.getActif());

        if (livreId != null) {
            Livre livre = livreRepository.findById(livreId)
                    .orElseThrow(() -> new RuntimeException("Livre non trouvé"));
            evenement.setLivre(livre);
        }

        if (auteurId != null) {
            Auteur auteur = auteurRepository.findById(auteurId)
                    .orElseThrow(() -> new RuntimeException("Auteur non trouvé"));
            evenement.setAuteur(auteur);
        }

        return EvenementDTO.fromEntity(evenementRepository.save(evenement));
    }

    public void delete(Long id) {
        evenementRepository.deleteById(id);
    }
}
