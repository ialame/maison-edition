package com.maisonedition.service;

import com.maisonedition.entity.Chapitre;
import com.maisonedition.entity.Livre;
import com.maisonedition.repository.ChapitreRepository;
import com.maisonedition.repository.LivreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapitreService {

    private final ChapitreRepository chapitreRepository;
    private final LivreRepository livreRepository;

    public List<Chapitre> findByLivreId(Long livreId) {
        return chapitreRepository.findByLivreIdOrderByNumeroAsc(livreId);
    }

    public List<Chapitre> findPubliesByLivreId(Long livreId) {
        return chapitreRepository.findByLivreIdAndPublieTrueOrderByNumeroAsc(livreId);
    }

    public List<Chapitre> findGratuitsByLivreId(Long livreId) {
        return chapitreRepository.findByLivreIdAndGratuitTrueAndPublieTrueOrderByNumeroAsc(livreId);
    }

    public Chapitre findById(Long id) {
        return chapitreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("الفصل غير موجود"));
    }

    public Chapitre findByLivreIdAndNumero(Long livreId, Integer numero) {
        return chapitreRepository.findByLivreIdAndNumeroWithLivre(livreId, numero)
                .orElseThrow(() -> new RuntimeException("الفصل غير موجود"));
    }

    @Transactional
    public Chapitre create(Long livreId, Chapitre chapitre) {
        Livre livre = livreRepository.findById(livreId)
                .orElseThrow(() -> new RuntimeException("الكتاب غير موجود"));

        // Auto-assign numero if not provided
        if (chapitre.getNumero() == null) {
            Integer maxNumero = chapitreRepository.findMaxNumeroByLivreId(livreId);
            chapitre.setNumero(maxNumero == null ? 1 : maxNumero + 1);
        }

        chapitre.setLivre(livre);
        return chapitreRepository.save(chapitre);
    }

    @Transactional
    public Chapitre update(Long id, Chapitre chapitreDetails) {
        Chapitre chapitre = findById(id);

        chapitre.setTitre(chapitreDetails.getTitre());
        chapitre.setContenu(chapitreDetails.getContenu());
        chapitre.setNumero(chapitreDetails.getNumero());
        chapitre.setGratuit(chapitreDetails.getGratuit());
        chapitre.setPublie(chapitreDetails.getPublie());

        return chapitreRepository.save(chapitre);
    }

    @Transactional
    public void delete(Long id) {
        Chapitre chapitre = findById(id);
        chapitreRepository.delete(chapitre);
    }

    @Transactional
    public void reorder(Long livreId, List<Long> chapitreIds) {
        int numero = 1;
        for (Long chapitreId : chapitreIds) {
            Chapitre chapitre = findById(chapitreId);
            if (!chapitre.getLivre().getId().equals(livreId)) {
                throw new RuntimeException("الفصل لا ينتمي لهذا الكتاب");
            }
            chapitre.setNumero(numero++);
            chapitreRepository.save(chapitre);
        }
    }

    public long countByLivreId(Long livreId) {
        return chapitreRepository.countByLivreId(livreId);
    }
}
