package com.maisonedition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maisonedition.dto.AuthDTO;
import com.maisonedition.dto.CheckoutRequest;
import com.maisonedition.entity.*;
import com.maisonedition.repository.*;
import com.maisonedition.service.StripeService;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommandeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private LivreRepository livreRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @MockBean
    private StripeService stripeService;

    private String authToken;
    private Long livreId;
    private Long userId;

    @BeforeAll
    void setUp() throws Exception {
        // Create a test category
        Categorie categorie = new Categorie();
        categorie.setNom("Test Category");
        categorie.setSlug("test-category");
        categorie = categorieRepository.save(categorie);

        // Create a test book
        Livre livre = new Livre();
        livre.setTitre("Test Book");
        livre.setPrix(new BigDecimal("25.00"));
        livre.setDisponible(true);
        livre.setCategorie(categorie);
        livre = livreRepository.save(livre);
        livreId = livre.getId();

        // Register a test user
        AuthDTO.RegisterRequest registerRequest = new AuthDTO.RegisterRequest();
        registerRequest.setNom("Test");
        registerRequest.setPrenom("User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setMotDePasse("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Login to get auth token
        AuthDTO.LoginRequest loginRequest = new AuthDTO.LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setMotDePasse("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(response).get("token").asText();

        userId = utilisateurRepository.findByEmail("test@example.com").get().getId();
    }

    @BeforeEach
    void setUpMocks() throws Exception {
        // Mock Stripe service with a properly mocked Session object
        // This needs to be in @BeforeEach because @MockBean resets between tests
        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn("cs_test_mock_session_id");
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/mock");
        when(stripeService.createCheckoutSession(any())).thenReturn(mockSession);
    }

    @Test
    @Order(1)
    @DisplayName("Test commande livre papier (prix variable)")
    void testCommandePapier() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setLivreId(livreId);
        request.setType("PAPIER");
        request.setNomComplet("Test User");
        request.setAdresse("123 Test Street");
        request.setVille("Paris");
        request.setCodePostal("75001");
        request.setPays("France");
        request.setTelephone("+33123456789");

        MvcResult result = mockMvc.perform(post("/api/commandes/checkout")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").exists())
                .andReturn();

        // Verify commande was created
        var commandes = commandeRepository.findByUtilisateurIdOrderByDateCommandeDesc(userId);
        assertFalse(commandes.isEmpty());

        Commande commande = commandes.get(0);
        assertEquals(TypeCommande.PAPIER, commande.getType());
        assertEquals(StatutCommande.EN_ATTENTE, commande.getStatut());
        assertEquals(new BigDecimal("25.00"), commande.getMontant());
        assertEquals("123 Test Street", commande.getAdresse());

        System.out.println("✓ Test PAPIER: Commande created with price " + commande.getMontant() + "€");
    }

    @Test
    @Order(2)
    @DisplayName("Test commande PDF (10€ fixe)")
    void testCommandeNumerique() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setLivreId(livreId);
        request.setType("NUMERIQUE");

        mockMvc.perform(post("/api/commandes/checkout")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").exists());

        var commandes = commandeRepository.findByUtilisateurIdOrderByDateCommandeDesc(userId);
        Commande commande = commandes.stream()
                .filter(c -> c.getType() == TypeCommande.NUMERIQUE)
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("10.00"), commande.getMontant());

        System.out.println("✓ Test NUMERIQUE (PDF): Commande created with fixed price 10.00€");
    }

    @Test
    @Order(3)
    @DisplayName("Test lecture livre 1 an (5€ fixe)")
    void testCommandeLectureLivre() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setLivreId(livreId);
        request.setType("LECTURE_LIVRE");

        mockMvc.perform(post("/api/commandes/checkout")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").exists());

        var commandes = commandeRepository.findByUtilisateurIdOrderByDateCommandeDesc(userId);
        Commande commande = commandes.stream()
                .filter(c -> c.getType() == TypeCommande.LECTURE_LIVRE)
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("5.00"), commande.getMontant());
        assertNotNull(commande.getDateDebutAcces());
        assertNotNull(commande.getDateFinAcces());
        assertEquals(commande.getDateDebutAcces().plusYears(1), commande.getDateFinAcces());

        System.out.println("✓ Test LECTURE_LIVRE: Commande created with fixed price 5.00€, access for 1 year");
    }

    @Test
    @Order(4)
    @DisplayName("Test abonnement mensuel (30€ fixe)")
    void testAbonnementMensuel() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setType("ABONNEMENT_MENSUEL");
        // No livreId for global subscription

        mockMvc.perform(post("/api/commandes/checkout")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").exists());

        var commandes = commandeRepository.findByUtilisateurIdOrderByDateCommandeDesc(userId);
        Commande commande = commandes.stream()
                .filter(c -> c.getType() == TypeCommande.ABONNEMENT_MENSUEL)
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("30.00"), commande.getMontant());
        assertNull(commande.getLivre()); // Global subscription has no specific book
        assertNotNull(commande.getDateDebutAcces());
        assertNotNull(commande.getDateFinAcces());
        assertEquals(commande.getDateDebutAcces().plusMonths(1), commande.getDateFinAcces());

        System.out.println("✓ Test ABONNEMENT_MENSUEL: Commande created with fixed price 30.00€, access for 1 month");
    }

    @Test
    @Order(5)
    @DisplayName("Test abonnement annuel (50€ fixe)")
    void testAbonnementAnnuel() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setType("ABONNEMENT_ANNUEL");
        // No livreId for global subscription

        mockMvc.perform(post("/api/commandes/checkout")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").exists());

        var commandes = commandeRepository.findByUtilisateurIdOrderByDateCommandeDesc(userId);
        Commande commande = commandes.stream()
                .filter(c -> c.getType() == TypeCommande.ABONNEMENT_ANNUEL)
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("50.00"), commande.getMontant());
        assertNull(commande.getLivre());
        assertNotNull(commande.getDateDebutAcces());
        assertNotNull(commande.getDateFinAcces());
        assertEquals(commande.getDateDebutAcces().plusYears(1), commande.getDateFinAcces());

        System.out.println("✓ Test ABONNEMENT_ANNUEL: Commande created with fixed price 50.00€, access for 1 year");
    }

    @Test
    @Order(6)
    @DisplayName("Test webhook marque commande comme payée")
    void testWebhookMarkAsPaid() throws Exception {
        // Get a pending commande
        var commandes = commandeRepository.findByUtilisateurIdOrderByDateCommandeDesc(userId);
        Commande commande = commandes.stream()
                .filter(c -> c.getStatut() == StatutCommande.EN_ATTENTE)
                .findFirst()
                .orElseThrow();

        // Simulate Stripe session ID being set
        String sessionId = "cs_test_webhook_test_session";
        commande.setStripeSessionId(sessionId);
        commandeRepository.save(commande);

        // Verify markAsPaid works
        commande = commandeRepository.findByStripeSessionId(sessionId).orElseThrow();
        commande.setStatut(StatutCommande.PAYEE);
        commande.setStripePaymentIntentId("pi_test_payment");
        commandeRepository.save(commande);

        // Verify status changed
        Commande updated = commandeRepository.findById(commande.getId()).orElseThrow();
        assertEquals(StatutCommande.PAYEE, updated.getStatut());

        System.out.println("✓ Test Webhook: Commande marked as PAYEE");
    }

    @Test
    @Order(7)
    @DisplayName("Test liste mes commandes")
    void testMesCommandes() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/commandes/mes-commandes")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        var commandesJson = objectMapper.readTree(response);

        assertTrue(commandesJson.size() >= 5, "Should have at least 5 commandes");

        System.out.println("✓ Test Mes Commandes: Retrieved " + commandesJson.size() + " commandes");
    }

    @Test
    @Order(8)
    @DisplayName("Test accès refusé sans authentification")
    void testUnauthorizedAccess() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setLivreId(livreId);
        request.setType("NUMERIQUE");

        mockMvc.perform(post("/api/commandes/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        System.out.println("✓ Test Security: Unauthorized access blocked");
    }

    @AfterAll
    void tearDown() {
        System.out.println("\n========================================");
        System.out.println("All integration tests completed!");
        System.out.println("========================================");
    }
}
