package com.maisonedition.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LatexConverterService {

    public String convertToHtml(String latex) {
        if (latex == null || latex.isEmpty()) {
            return "";
        }

        String html = latex;

        // === STEP 1: Extract and protect math formulas before any conversion ===
        List<String> mathBlocks = new ArrayList<>();

        // Extract display math $$...$$ (must be before inline $...$)
        Pattern displayMathPattern = Pattern.compile("\\$\\$(.*?)\\$\\$", Pattern.DOTALL);
        Matcher displayMatcher = displayMathPattern.matcher(html);
        StringBuffer sb1 = new StringBuffer();
        while (displayMatcher.find()) {
            String mathContent = displayMatcher.group(1).trim();
            int index = mathBlocks.size();
            mathBlocks.add("DISPLAY:" + mathContent);
            displayMatcher.appendReplacement(sb1, Matcher.quoteReplacement("%%MATH_BLOCK_" + index + "%%"));
        }
        displayMatcher.appendTail(sb1);
        html = sb1.toString();

        // Extract \[...\] display math
        Pattern displayMathPattern2 = Pattern.compile("\\\\\\[(.*?)\\\\\\]", Pattern.DOTALL);
        Matcher displayMatcher2 = displayMathPattern2.matcher(html);
        StringBuffer sb1b = new StringBuffer();
        while (displayMatcher2.find()) {
            String mathContent = displayMatcher2.group(1).trim();
            int index = mathBlocks.size();
            mathBlocks.add("DISPLAY:" + mathContent);
            displayMatcher2.appendReplacement(sb1b, Matcher.quoteReplacement("%%MATH_BLOCK_" + index + "%%"));
        }
        displayMatcher2.appendTail(sb1b);
        html = sb1b.toString();

        // Extract inline math $...$
        Pattern inlineMathPattern = Pattern.compile("(?<!\\\\)\\$([^$]+?)\\$");
        Matcher inlineMatcher = inlineMathPattern.matcher(html);
        StringBuffer sb2 = new StringBuffer();
        while (inlineMatcher.find()) {
            String mathContent = inlineMatcher.group(1).trim();
            int index = mathBlocks.size();
            mathBlocks.add("INLINE:" + mathContent);
            inlineMatcher.appendReplacement(sb2, Matcher.quoteReplacement("%%MATH_BLOCK_" + index + "%%"));
        }
        inlineMatcher.appendTail(sb2);
        html = sb2.toString();

        // Extract \(...\) inline math
        Pattern inlineMathPattern2 = Pattern.compile("\\\\\\((.*?)\\\\\\)", Pattern.DOTALL);
        Matcher inlineMatcher2 = inlineMathPattern2.matcher(html);
        StringBuffer sb2b = new StringBuffer();
        while (inlineMatcher2.find()) {
            String mathContent = inlineMatcher2.group(1).trim();
            int index = mathBlocks.size();
            mathBlocks.add("INLINE:" + mathContent);
            inlineMatcher2.appendReplacement(sb2b, Matcher.quoteReplacement("%%MATH_BLOCK_" + index + "%%"));
        }
        inlineMatcher2.appendTail(sb2b);
        html = sb2b.toString();

        // === STEP 2: Normal LaTeX to HTML conversion ===

        // Remove preamble (everything before \begin{document})
        html = html.replaceAll("(?s).*?\\\\begin\\{document\\}", "");
        html = html.replaceAll("\\\\end\\{document\\}", "");

        // Remove common preamble commands that might remain
        html = html.replaceAll("\\\\documentclass\\[.*?\\]\\{.*?\\}", "");
        html = html.replaceAll("\\\\documentclass\\{.*?\\}", "");
        html = html.replaceAll("\\\\usepackage\\[.*?\\]\\{.*?\\}", "");
        html = html.replaceAll("\\\\usepackage\\{.*?\\}", "");
        html = html.replaceAll("\\\\input\\{.*?\\}", "");
        html = html.replaceAll("\\\\title\\{.*?\\}", "");
        html = html.replaceAll("\\\\author\\{.*?\\}", "");
        html = html.replaceAll("\\\\date\\{.*?\\}", "");
        html = html.replaceAll("\\\\maketitle", "");
        html = html.replaceAll("\\\\tableofcontents", "");
        html = html.replaceAll("\\\\newpage", "");
        html = html.replaceAll("\\\\clearpage", "");
        html = html.replaceAll("\\\\pagebreak", "");

        // Convert chapters and sections
        html = html.replaceAll("\\\\chapter\\*?\\{([^}]*)\\}", "<h1 class=\"chapter\">$1</h1>");
        html = html.replaceAll("\\\\section\\*?\\{([^}]*)\\}", "<h2 class=\"section\">$1</h2>");
        html = html.replaceAll("\\\\subsection\\*?\\{([^}]*)\\}", "<h3 class=\"subsection\">$1</h3>");
        html = html.replaceAll("\\\\subsubsection\\*?\\{([^}]*)\\}", "<h4 class=\"subsubsection\">$1</h4>");
        html = html.replaceAll("\\\\paragraph\\*?\\{([^}]*)\\}", "<h5 class=\"paragraph\">$1</h5>");

        // Convert text formatting
        html = html.replaceAll("\\\\textbf\\{([^}]*)\\}", "<strong>$1</strong>");
        html = html.replaceAll("\\\\textit\\{([^}]*)\\}", "<em>$1</em>");
        html = html.replaceAll("\\\\underline\\{([^}]*)\\}", "<u>$1</u>");
        html = html.replaceAll("\\\\emph\\{([^}]*)\\}", "<em>$1</em>");
        html = html.replaceAll("\\\\textrm\\{([^}]*)\\}", "$1");
        html = html.replaceAll("\\\\textsf\\{([^}]*)\\}", "$1");
        html = html.replaceAll("\\\\texttt\\{([^}]*)\\}", "<code>$1</code>");
        html = html.replaceAll("\\\\textsc\\{([^}]*)\\}", "<span style=\"font-variant: small-caps\">$1</span>");

        // Font sizes (approximate conversion)
        html = html.replaceAll("\\\\tiny\\b", "");
        html = html.replaceAll("\\\\scriptsize\\b", "");
        html = html.replaceAll("\\\\footnotesize\\b", "");
        html = html.replaceAll("\\\\small\\b", "");
        html = html.replaceAll("\\\\normalsize\\b", "");
        html = html.replaceAll("\\\\large\\b", "");
        html = html.replaceAll("\\\\Large\\b", "");
        html = html.replaceAll("\\\\LARGE\\b", "");
        html = html.replaceAll("\\\\huge\\b", "");
        html = html.replaceAll("\\\\Huge\\b", "");

        // Convert images to placeholders with visible marker
        // Handle \includegraphics[options]{filename}
        Pattern imgPattern = Pattern.compile("\\\\includegraphics(?:\\[.*?\\])?\\{([^}]*)\\}");
        Matcher imgMatcher = imgPattern.matcher(html);
        StringBuffer imgSb = new StringBuffer();
        while (imgMatcher.find()) {
            String filename = imgMatcher.group(1);
            // Clean up the filename
            filename = filename.replaceAll(".*/", ""); // Remove path
            String placeholder = "<div class=\"image-placeholder\" style=\"background: #fef3c7; border: 2px dashed #d97706; padding: 2rem; margin: 1.5rem 0; text-align: center; border-radius: 0.5rem;\">" +
                    "<p style=\"color: #92400e; font-weight: bold; margin: 0;\">📷 [IMAGE: " + filename + "]</p>" +
                    "</div>";
            imgMatcher.appendReplacement(imgSb, Matcher.quoteReplacement(placeholder));
        }
        imgMatcher.appendTail(imgSb);
        html = imgSb.toString();

        // Convert figure environment
        html = html.replaceAll("\\\\begin\\{figure\\}\\[.*?\\]", "<figure>");
        html = html.replaceAll("\\\\begin\\{figure\\}", "<figure>");
        html = html.replaceAll("\\\\end\\{figure\\}", "</figure>");
        html = html.replaceAll("\\\\caption\\{([^}]*)\\}", "<figcaption>$1</figcaption>");
        html = html.replaceAll("\\\\label\\{[^}]*\\}", "");

        // Convert lists
        html = html.replaceAll("\\\\begin\\{itemize\\}", "<ul>");
        html = html.replaceAll("\\\\end\\{itemize\\}", "</ul>");
        html = html.replaceAll("\\\\begin\\{enumerate\\}", "<ol>");
        html = html.replaceAll("\\\\end\\{enumerate\\}", "</ol>");
        html = html.replaceAll("\\\\item\\s*", "<li>");

        // Convert quote/quotation environments
        html = html.replaceAll("\\\\begin\\{quote\\}", "<blockquote>");
        html = html.replaceAll("\\\\end\\{quote\\}", "</blockquote>");
        html = html.replaceAll("\\\\begin\\{quotation\\}", "<blockquote>");
        html = html.replaceAll("\\\\end\\{quotation\\}", "</blockquote>");

        // Convert center environment
        html = html.replaceAll("\\\\begin\\{center\\}", "<div style=\"text-align: center\">");
        html = html.replaceAll("\\\\end\\{center\\}", "</div>");

        // Convert flushright/flushleft
        html = html.replaceAll("\\\\begin\\{flushright\\}", "<div style=\"text-align: right\">");
        html = html.replaceAll("\\\\end\\{flushright\\}", "</div>");
        html = html.replaceAll("\\\\begin\\{flushleft\\}", "<div style=\"text-align: left\">");
        html = html.replaceAll("\\\\end\\{flushleft\\}", "</div>");

        // Spacing commands
        html = html.replaceAll("\\\\hspace\\*?\\{[^}]*\\}", "&nbsp;&nbsp;");
        html = html.replaceAll("\\\\vspace\\*?\\{[^}]*\\}", "<div style=\"margin: 1rem 0\"></div>");
        html = html.replaceAll("\\\\bigskip", "<div style=\"margin: 1.5rem 0\"></div>");
        html = html.replaceAll("\\\\medskip", "<div style=\"margin: 1rem 0\"></div>");
        html = html.replaceAll("\\\\smallskip", "<div style=\"margin: 0.5rem 0\"></div>");
        html = html.replaceAll("\\\\\\\\", "<br>");
        html = html.replaceAll("\\\\newline", "<br>");
        html = html.replaceAll("\\\\linebreak", "<br>");
        html = html.replaceAll("\\\\noindent", "");

        // Special characters
        html = html.replaceAll("\\\\&", "&amp;");
        html = html.replaceAll("\\\\%", "%");
        html = html.replaceAll("\\\\\\$", "$");
        html = html.replaceAll("\\\\#", "#");
        html = html.replaceAll("\\\\_", "_");
        html = html.replaceAll("\\\\\\{", "{");
        html = html.replaceAll("\\\\\\}", "}");
        html = html.replaceAll("``", "\"");
        html = html.replaceAll("''", "\"");
        html = html.replaceAll("`", "'");
        html = html.replaceAll("---", "—");
        html = html.replaceAll("--", "–");
        html = html.replaceAll("\\\\ldots", "...");
        html = html.replaceAll("\\\\dots", "...");
        html = html.replaceAll("~", "&nbsp;");

        // Arabic-specific commands (common in Arabic LaTeX)
        html = html.replaceAll("\\\\LR\\{([^}]*)\\}", "<span dir=\"ltr\">$1</span>");
        html = html.replaceAll("\\\\RL\\{([^}]*)\\}", "<span dir=\"rtl\">$1</span>");
        html = html.replaceAll("\\\\textenglish\\{([^}]*)\\}", "<span dir=\"ltr\">$1</span>");
        html = html.replaceAll("\\\\textarabic\\{([^}]*)\\}", "$1");

        // Custom commands for foreign languages
        html = html.replaceAll("\\\\newcommand\\{[^}]*\\}\\[[^\\]]*\\]\\{[^}]*\\}", ""); // Remove \newcommand definitions
        html = html.replaceAll("\\\\newcommand\\{[^}]*\\}\\{[^}]*\\}", ""); // Remove \newcommand without args
        html = html.replaceAll("\\\\fr\\{([^}]*)\\}", "<span dir=\"ltr\" lang=\"fr\">$1</span>");
        html = html.replaceAll("\\\\foreignlanguage\\{french\\}\\{([^}]*)\\}", "<span dir=\"ltr\" lang=\"fr\">$1</span>");
        html = html.replaceAll("\\\\foreignlanguage\\{english\\}\\{([^}]*)\\}", "<span dir=\"ltr\" lang=\"en\">$1</span>");
        html = html.replaceAll("\\\\foreignlanguage\\{arabic\\}\\{([^}]*)\\}", "<span dir=\"rtl\" lang=\"ar\">$1</span>");
        html = html.replaceAll("\\\\foreignlanguage\\{[^}]*\\}\\{([^}]*)\\}", "<span>$1</span>"); // Generic fallback

        // Footnotes - convert to parenthetical note
        html = html.replaceAll("\\\\footnote\\{([^}]*)\\}", " <span class=\"footnote\" style=\"font-size: 0.85em; color: #666\">($1)</span>");

        // Remove remaining LaTeX commands we don't handle
        html = html.replaceAll("\\\\centering", "");
        html = html.replaceAll("\\\\raggedright", "");
        html = html.replaceAll("\\\\raggedleft", "");
        html = html.replaceAll("\\\\selectlanguage\\{[^}]*\\}", "");
        html = html.replaceAll("\\\\setlength\\{[^}]*\\}\\{[^}]*\\}", "");

        // Remove any remaining simple commands
        html = html.replaceAll("\\\\[a-zA-Z]+\\*?(?:\\[[^\\]]*\\])?(?:\\{\\})?(?=\\s|$|[^{])", "");

        // Convert paragraphs (double newlines to paragraph tags)
        // First normalize line endings
        html = html.replaceAll("\r\n", "\n");
        html = html.replaceAll("\r", "\n");

        // Split by double newlines and wrap in paragraphs
        String[] paragraphs = html.split("\n\\s*\n");
        StringBuilder result = new StringBuilder();

        for (String para : paragraphs) {
            para = para.trim();
            if (!para.isEmpty()) {
                // Don't wrap if it's already a block element
                if (para.startsWith("<h") || para.startsWith("<ul") || para.startsWith("<ol") ||
                    para.startsWith("<blockquote") || para.startsWith("<div") || para.startsWith("<figure") ||
                    para.startsWith("<li")) {
                    result.append(para).append("\n");
                } else {
                    result.append("<p>").append(para).append("</p>\n");
                }
            }
        }

        // Clean up extra whitespace
        html = result.toString();
        html = html.replaceAll("\\s+", " ");
        html = html.replaceAll("> <", "><");
        html = html.replaceAll("<p>\\s*</p>", "");
        html = html.replaceAll("<p><(h[1-6]|ul|ol|blockquote|div|figure)", "<$1");
        html = html.replaceAll("</(h[1-6]|ul|ol|blockquote|div|figure)></p>", "</$1>");

        // === STEP 3: Restore math formulas with delimiters (KaTeX renders them in frontend) ===
        for (int i = 0; i < mathBlocks.size(); i++) {
            String block = mathBlocks.get(i);
            String placeholder = "%%MATH_BLOCK_" + i + "%%";
            if (block.startsWith("DISPLAY:")) {
                String formula = block.substring("DISPLAY:".length());
                formula = convertMathForKatex(formula);
                html = html.replace(placeholder, "$$" + formula + "$$");
            } else {
                String formula = block.substring("INLINE:".length());
                formula = convertMathForKatex(formula);
                html = html.replace(placeholder, "$" + formula + "$");
            }
        }

        return html.trim();
    }

    /**
     * Convert LaTeX math commands to KaTeX-compatible equivalents.
     */
    private String convertMathForKatex(String formula) {
        // Replace \mbox{...} with \text{...} (KaTeX supports \text)
        formula = formula.replaceAll("\\\\mbox\\{", "\\\\text{");
        // Replace \hbox{...} with \text{...}
        formula = formula.replaceAll("\\\\hbox\\{", "\\\\text{");
        return formula;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    private String escapeHtmlAttr(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
