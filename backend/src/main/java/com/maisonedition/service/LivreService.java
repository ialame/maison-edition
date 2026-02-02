package com.maisonedition.service;

import com.maisonedition.dto.LivreDTO;
import com.maisonedition.entity.Auteur;
import com.maisonedition.entity.Categorie;
import com.maisonedition.entity.Livre;
import com.maisonedition.repository.AuteurRepository;
import com.maisonedition.repository.CategorieRepository;
import com.maisonedition.repository.LivreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LivreService {

    private final LivreRepository livreRepository;
    private final AuteurRepository auteurRepository;
    private final CategorieRepository categorieRepository;

    public List<LivreDTO> findAll() {
        return livreRepository.findAll().stream()
                .map(LivreDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<LivreDTO> findAll(Pageable pageable) {
        return livreRepository.findAll(pageable).map(LivreDTO::fromEntity);
    }

    public LivreDTO findById(Long id) {
        return livreRepository.findById(id)
                .map(LivreDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Livre non trouvé avec l'id: " + id));
    }

    public List<LivreDTO> findEnVedette() {
        return livreRepository.findByEnVedetteTrue().stream()
                .map(LivreDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<LivreDTO> findNouveautes(int limit) {
        return livreRepository.findDernieresNouveautes(PageRequest.of(0, limit)).stream()
                .map(LivreDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<LivreDTO> findByCategorie(Long categorieId, Pageable pageable) {
        return livreRepository.findByCategorieId(categorieId, pageable).map(LivreDTO::fromEntity);
    }

    public List<LivreDTO> findByAuteur(Long auteurId) {
        return livreRepository.findByAuteurId(auteurId).stream()
                .map(LivreDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<LivreDTO> rechercher(String terme, Pageable pageable) {
        return livreRepository.rechercher(terme, pageable).map(LivreDTO::fromEntity);
    }

    public LivreDTO create(LivreDTO dto, List<Long> auteurIds, Long categorieId) {
        Livre livre = Livre.builder()
                .titre(dto.getTitre())
                .isbn(dto.getIsbn())
                .description(dto.getDescription())
                .resume(dto.getResume())
                .couverture(dto.getCouverture())
                .prix(dto.getPrix())
                .prixNumerique(dto.getPrixNumerique())
                .prixAbonnementMensuel(dto.getPrixAbonnementMensuel())
                .prixAbonnementAnnuel(dto.getPrixAbonnementAnnuel())
                .nombrePages(dto.getNombrePages())
                .datePublication(dto.getDatePublication())
                .langue(dto.getLangue())
                .format(dto.getFormat())
                .disponible(dto.getDisponible() != null ? dto.getDisponible() : true)
                .enVedette(dto.getEnVedette() != null ? dto.getEnVedette() : false)
                .stock(dto.getStock() != null ? dto.getStock() : 0)
                .seuilAlerte(dto.getSeuilAlerte() != null ? dto.getSeuilAlerte() : 5)
                .build();

        if (auteurIds != null && !auteurIds.isEmpty()) {
            List<Auteur> auteurs = auteurRepository.findAllById(auteurIds);
            livre.setAuteurs(auteurs);
        }

        if (categorieId != null) {
            Categorie categorie = categorieRepository.findById(categorieId)
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            livre.setCategorie(categorie);
        }

        return LivreDTO.fromEntity(livreRepository.save(livre));
    }

    public LivreDTO update(Long id, LivreDTO dto, List<Long> auteurIds, Long categorieId) {
        Livre livre = livreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livre non trouvé avec l'id: " + id));

        livre.setTitre(dto.getTitre());
        livre.setIsbn(dto.getIsbn());
        livre.setDescription(dto.getDescription());
        livre.setResume(dto.getResume());
        livre.setCouverture(dto.getCouverture());
        livre.setPrix(dto.getPrix());
        livre.setPrixNumerique(dto.getPrixNumerique());
        livre.setPrixAbonnementMensuel(dto.getPrixAbonnementMensuel());
        livre.setPrixAbonnementAnnuel(dto.getPrixAbonnementAnnuel());
        livre.setNombrePages(dto.getNombrePages());
        livre.setDatePublication(dto.getDatePublication());
        livre.setLangue(dto.getLangue());
        livre.setFormat(dto.getFormat());
        livre.setDisponible(dto.getDisponible());
        livre.setEnVedette(dto.getEnVedette());
        if (dto.getStock() != null) {
            livre.setStock(dto.getStock());
        }
        if (dto.getSeuilAlerte() != null) {
            livre.setSeuilAlerte(dto.getSeuilAlerte());
        }

        if (auteurIds != null) {
            List<Auteur> auteurs = auteurRepository.findAllById(auteurIds);
            livre.setAuteurs(auteurs);
        }

        if (categorieId != null) {
            Categorie categorie = categorieRepository.findById(categorieId)
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            livre.setCategorie(categorie);
        }

        return LivreDTO.fromEntity(livreRepository.save(livre));
    }

    public void delete(Long id) {
        livreRepository.deleteById(id);
    }

    public LivreDTO updateStock(Long id, Integer stock, Integer seuilAlerte) {
        Livre livre = livreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livre non trouvé avec l'id: " + id));

        livre.setStock(stock);
        if (seuilAlerte != null) {
            livre.setSeuilAlerte(seuilAlerte);
        }

        // Auto-update disponible based on stock
        livre.setDisponible(stock > 0);

        return LivreDTO.fromEntity(livreRepository.save(livre));
    }

    public List<LivreDTO> findStockBas() {
        return livreRepository.findLivresStockBas().stream()
                .map(LivreDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<LivreDTO> findRuptureStock() {
        return livreRepository.findLivresRuptureStock().stream()
                .map(LivreDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getStockStats() {
        return Map.of(
                "stockBas", livreRepository.countLivresStockBas(),
                "ruptureStock", livreRepository.findLivresRuptureStock().size()
        );
    }

    public LivreDTO updatePdfPath(Long id, String pdfPath) {
        Livre livre = livreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livre non trouvé avec l'id: " + id));
        livre.setPdfPath(pdfPath);
        return LivreDTO.fromEntity(livreRepository.save(livre));
    }
}
