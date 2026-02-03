package com.maisonedition.service;

import com.maisonedition.entity.Chapitre;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TTSService {

    private final ChapitreService chapitreService;

    @Value("${google.tts.api.key}")
    private String apiKey;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final String GOOGLE_TTS_URL = "https://texttospeech.googleapis.com/v1/text:synthesize";
    private static final String TTS_CACHE_DIR = "tts";

    /**
     * Get or generate audio for a chapter
     * @return Path to the MP3 file relative to uploads dir
     */
    public String getOrGenerateAudio(Long livreId, Integer chapitreNumero) throws IOException {
        Chapitre chapitre = chapitreService.findByLivreIdAndNumero(livreId, chapitreNumero);

        // Generate cache filename based on content hash
        String contentHash = hashContent(chapitre.getContenu());
        String filename = String.format("chapitre_%d_%d_%s.mp3", livreId, chapitreNumero, contentHash.substring(0, 8));
        String relativePath = TTS_CACHE_DIR + "/" + filename;
        Path fullPath = Paths.get(uploadDir, TTS_CACHE_DIR, filename);

        // Check if cached file exists
        if (Files.exists(fullPath)) {
            log.info("TTS cache hit for chapitre {}/{}", livreId, chapitreNumero);
            return relativePath;
        }

        // Generate audio via Google TTS
        log.info("Generating TTS for chapitre {}/{}", livreId, chapitreNumero);
        byte[] audioContent = callGoogleTTS(chapitre.getContenu());

        // Ensure directory exists
        Files.createDirectories(fullPath.getParent());

        // Save to cache
        Files.write(fullPath, audioContent);
        log.info("TTS saved to cache: {}", relativePath);

        return relativePath;
    }

    /**
     * Check if audio exists in cache
     */
    public boolean hasAudioCache(Long livreId, Integer chapitreNumero) {
        try {
            Chapitre chapitre = chapitreService.findByLivreIdAndNumero(livreId, chapitreNumero);
            String contentHash = hashContent(chapitre.getContenu());
            String filename = String.format("chapitre_%d_%d_%s.mp3", livreId, chapitreNumero, contentHash.substring(0, 8));
            Path fullPath = Paths.get(uploadDir, TTS_CACHE_DIR, filename);
            return Files.exists(fullPath);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] callGoogleTTS(String htmlContent) throws IOException {
        // Convert HTML to SSML with pauses
        String ssml = convertToSSML(htmlContent);

        RestTemplate restTemplate = new RestTemplate();

        // Build request body with SSML
        Map<String, Object> input = new HashMap<>();
        input.put("ssml", ssml);

        Map<String, Object> voice = new HashMap<>();
        voice.put("languageCode", "ar-XA");
        voice.put("name", "ar-XA-Wavenet-B"); // High quality Arabic male voice
        voice.put("ssmlGender", "MALE");

        Map<String, Object> audioConfig = new HashMap<>();
        audioConfig.put("audioEncoding", "MP3");
        audioConfig.put("speakingRate", 0.9); // Slightly slower for clarity
        audioConfig.put("pitch", 0.0);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input", input);
        requestBody.put("voice", voice);
        requestBody.put("audioConfig", audioConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = GOOGLE_TTS_URL + "?key=" + apiKey;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String audioContentBase64 = (String) response.getBody().get("audioContent");
                return Base64.getDecoder().decode(audioContentBase64);
            } else {
                throw new IOException("Google TTS API error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Google TTS API call failed", e);
            throw new IOException("Failed to generate audio: " + e.getMessage(), e);
        }
    }

    /**
     * Convert HTML content to SSML with natural pauses
     */
    private String convertToSSML(String htmlContent) {
        // Replace paragraph and div endings with long pauses
        String text = htmlContent
            .replaceAll("</p>", " <break time=\"800ms\"/> ")
            .replaceAll("</div>", " <break time=\"600ms\"/> ")
            .replaceAll("<br\\s*/?>", " <break time=\"400ms\"/> ")
            .replaceAll("</h[1-6]>", " <break time=\"1000ms\"/> ");

        // Strip remaining HTML tags
        text = text.replaceAll("<[^>]*>", " ");

        // Add pauses after punctuation (Arabic and Latin)
        // Long pause after period, question mark, exclamation (، . ؟ ! 。)
        text = text.replaceAll("([.!?؟。])(\\s+)", "$1 <break time=\"600ms\"/> ");

        // Medium pause after colon and semicolon
        text = text.replaceAll("([:;؛])(\\s+)", "$1 <break time=\"400ms\"/> ");

        // Short pause after comma (، ,)
        text = text.replaceAll("([,،])(\\s+)", "$1 <break time=\"250ms\"/> ");

        // Clean up multiple spaces
        text = text.replaceAll("\\s+", " ").trim();

        // Limit length (SSML has ~5000 char limit for the text content)
        if (text.length() > 4500) {
            text = text.substring(0, 4500);
        }

        // Wrap in SSML speak tags
        return "<speak>" + text + "</speak>";
    }

    private String hashContent(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return String.valueOf(content.hashCode());
        }
    }

    /**
     * Invalidate cache for a chapter (call when chapter content is updated)
     */
    public void invalidateCache(Long livreId, Integer chapitreNumero) {
        try {
            Path ttsDir = Paths.get(uploadDir, TTS_CACHE_DIR);
            if (Files.exists(ttsDir)) {
                String prefix = String.format("chapitre_%d_%d_", livreId, chapitreNumero);
                Files.list(ttsDir)
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            log.info("Deleted TTS cache: {}", p);
                        } catch (IOException e) {
                            log.warn("Failed to delete TTS cache: {}", p);
                        }
                    });
            }
        } catch (IOException e) {
            log.warn("Failed to invalidate TTS cache", e);
        }
    }
}
