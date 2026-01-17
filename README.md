# Maison d'Édition

Site web complet pour une maison d'édition avec un catalogue de livres, un blog, des événements et une interface d'administration.

## Stack technique

### Backend
- **Spring Boot 3.2** - Framework Java
- **MariaDB** - Base de données relationnelle
- **Spring Security + JWT** - Authentification
- **Spring Data JPA** - ORM
- **Lombok** - Réduction du code boilerplate
- **Springdoc OpenAPI** - Documentation API (Swagger)

### Frontend
- **Vue.js 3** - Framework JavaScript avec Composition API
- **TypeScript** - Typage statique
- **Vite** - Build tool
- **Vue Router** - Routage
- **Pinia** - State management
- **Tailwind CSS** - Framework CSS
- **Axios** - Client HTTP

## Structure du projet

```
maison-edition/
├── backend/                    # API Spring Boot
│   ├── src/main/java/com/maisonedition/
│   │   ├── config/            # Configuration (Security, CORS)
│   │   ├── controller/        # Contrôleurs REST
│   │   ├── dto/               # Data Transfer Objects
│   │   ├── entity/            # Entités JPA
│   │   ├── repository/        # Repositories Spring Data
│   │   ├── security/          # JWT et authentification
│   │   └── service/           # Services métier
│   └── src/main/resources/
│       └── application.properties
│
└── frontend/                   # Application Vue.js
    ├── src/
    │   ├── assets/            # Styles CSS
    │   ├── components/        # Composants réutilisables
    │   ├── router/            # Configuration du routage
    │   ├── services/          # Services API
    │   ├── stores/            # Stores Pinia
    │   ├── types/             # Types TypeScript
    │   └── views/             # Pages de l'application
    │       ├── public/        # Pages publiques
    │       └── admin/         # Interface d'administration
    └── index.html
```

## Fonctionnalités

### Site public
- **Accueil** - Livres en vedette, nouveautés, actualités, événements
- **Catalogue** - Liste paginée des livres avec filtres par catégorie et recherche
- **Détail livre** - Informations complètes sur un livre
- **Auteurs** - Liste et profils des auteurs
- **Blog** - Articles et actualités littéraires
- **Événements** - Dédicaces, salons, conférences
- **À propos** - Présentation de la maison d'édition
- **Contact** - Formulaire de contact

### Administration
- **Tableau de bord** - Statistiques et actions rapides
- **Gestion des livres** - CRUD complet avec auteurs et catégories
- **Gestion des auteurs** - CRUD avec biographie
- **Gestion des catégories** - CRUD hiérarchique
- **Gestion des articles** - CRUD avec workflow (brouillon/publié/archivé)
- **Gestion des événements** - CRUD avec association livre/auteur

## Installation

### Prérequis
- Java 17+
- Node.js 18+
- MariaDB 10.6+

### Base de données

```sql
CREATE DATABASE maison_edition CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

L'API sera disponible sur `http://localhost:8080`

Documentation Swagger : `http://localhost:8080/swagger-ui.html`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

L'application sera disponible sur `http://localhost:5173`

## Configuration

### Backend (application.properties)

```properties
# Base de données
spring.datasource.url=jdbc:mariadb://localhost:3306/maison_edition
spring.datasource.username=root
spring.datasource.password=votre_mot_de_passe

# JWT
app.jwt.secret=VotreCleSecreteTresLongue
app.jwt.expiration=86400000
```

### Créer un utilisateur admin

```sql
INSERT INTO utilisateurs (email, mot_de_passe, nom, prenom, role, actif, date_creation, date_modification)
VALUES (
  'admin@maison-edition.fr',
  '$2a$10$...', -- BCrypt hash de votre mot de passe
  'Admin',
  'Super',
  'ADMIN',
  true,
  NOW(),
  NOW()
);
```

## API Endpoints

### Authentification
- `POST /api/auth/login` - Connexion
- `POST /api/auth/register` - Inscription

### Livres (publics)
- `GET /api/livres` - Liste paginée
- `GET /api/livres/{id}` - Détail
- `GET /api/livres/vedette` - En vedette
- `GET /api/livres/nouveautes` - Nouveautés
- `GET /api/livres/recherche?q=` - Recherche

### Auteurs (publics)
- `GET /api/auteurs` - Liste
- `GET /api/auteurs/{id}` - Détail

### Catégories (publics)
- `GET /api/categories` - Liste

### Articles (publics)
- `GET /api/articles` - Articles publiés
- `GET /api/articles/slug/{slug}` - Par slug

### Événements (publics)
- `GET /api/evenements/a-venir` - À venir

## Licence

Projet privé - Tous droits réservés.
