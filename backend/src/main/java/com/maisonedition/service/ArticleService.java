package com.maisonedition.service;

import com.maisonedition.dto.ArticleDTO;
import com.maisonedition.entity.Article;
import com.maisonedition.entity.Tag;
import com.maisonedition.entity.Utilisateur;
import com.maisonedition.repository.ArticleRepository;
import com.maisonedition.repository.TagRepository;
import com.maisonedition.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TagRepository tagRepository;

    public List<ArticleDTO> findAll() {
        return articleRepository.findAll().stream()
                .map(ArticleDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<ArticleDTO> findPublies(Pageable pageable) {
        return articleRepository.findByStatutOrderByDatePublicationDesc(Article.Statut.PUBLIE, pageable)
                .map(ArticleDTO::fromEntity);
    }

    public List<ArticleDTO> findDerniersPublies() {
        return articleRepository.findTop5ByStatutOrderByDatePublicationDesc(Article.Statut.PUBLIE).stream()
                .map(ArticleDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public ArticleDTO findById(Long id) {
        return articleRepository.findById(id)
                .map(ArticleDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Article non trouvé avec l'id: " + id));
    }

    public ArticleDTO findBySlug(String slug) {
        return articleRepository.findBySlug(slug)
                .map(ArticleDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Article non trouvé avec le slug: " + slug));
    }

    public ArticleDTO findBySlugAndIncrementVues(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Article non trouvé avec le slug: " + slug));
        article.setNombreVues(article.getNombreVues() + 1);
        return ArticleDTO.fromEntity(articleRepository.save(article));
    }

    public ArticleDTO create(ArticleDTO dto, Long auteurId) {
        Article article = Article.builder()
                .titre(dto.getTitre())
                .slug(dto.getSlug())
                .chapeau(dto.getChapeau())
                .contenu(dto.getContenu())
                .imagePrincipale(dto.getImagePrincipale())
                .statut(Article.Statut.BROUILLON)
                .build();

        if (auteurId != null) {
            Utilisateur auteur = utilisateurRepository.findById(auteurId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            article.setAuteur(auteur);
        }

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(dto.getTagIds());
            article.setTags(new java.util.HashSet<>(tags));
        }

        return ArticleDTO.fromEntity(articleRepository.save(article));
    }

    public ArticleDTO update(Long id, ArticleDTO dto) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article non trouvé avec l'id: " + id));

        article.setTitre(dto.getTitre());
        article.setSlug(dto.getSlug());
        article.setChapeau(dto.getChapeau());
        article.setContenu(dto.getContenu());
        article.setImagePrincipale(dto.getImagePrincipale());

        if (dto.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(dto.getTagIds());
            article.setTags(new java.util.HashSet<>(tags));
        }

        return ArticleDTO.fromEntity(articleRepository.save(article));
    }

    public ArticleDTO publier(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article non trouvé avec l'id: " + id));

        article.setStatut(Article.Statut.PUBLIE);
        article.setDatePublication(LocalDateTime.now());

        return ArticleDTO.fromEntity(articleRepository.save(article));
    }

    public ArticleDTO archiver(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article non trouvé avec l'id: " + id));

        article.setStatut(Article.Statut.ARCHIVE);

        return ArticleDTO.fromEntity(articleRepository.save(article));
    }

    public void delete(Long id) {
        articleRepository.deleteById(id);
    }

    public Page<ArticleDTO> findByTag(String tagSlug, Pageable pageable) {
        return articleRepository.findByTagSlugAndStatut(tagSlug, Article.Statut.PUBLIE, pageable)
                .map(ArticleDTO::fromEntity);
    }

    public Page<ArticleDTO> search(String query, Pageable pageable) {
        return articleRepository.searchByTitreOrChapeau(query, Article.Statut.PUBLIE, pageable)
                .map(ArticleDTO::fromEntity);
    }

    public List<ArticleDTO> findRelated(Long articleId, int limit) {
        return articleRepository.findRelatedArticles(articleId, Article.Statut.PUBLIE,
                org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(ArticleDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
