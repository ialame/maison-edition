package com.maisonedition.controller;

import com.maisonedition.service.LatexConverterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class LatexController {

    private final LatexConverterService latexConverterService;

    @PostMapping("/convert-latex")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITEUR')")
    public ResponseEntity<Map<String, String>> convertLatex(@RequestBody Map<String, String> request) {
        String latex = request.get("latex");
        if (latex == null || latex.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le contenu LaTeX est requis"));
        }

        String html = latexConverterService.convertToHtml(latex);
        return ResponseEntity.ok(Map.of("html", html));
    }
}
