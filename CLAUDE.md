# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Maison d'Édition is a full-stack publishing house web application with book catalog, e-reading, blog, events, and e-commerce with Stripe payments. The site is in French/Arabic with RTL support.

**Tech Stack:** Spring Boot 3.2 (Java 17) + Vue 3 (TypeScript) + MariaDB + Tailwind CSS

## Common Commands

### Backend (from `/backend`)
```bash
./mvnw spring-boot:run                  # Run dev server (port 8080)
./mvnw clean package                    # Build JAR
./mvnw clean package -DskipTests        # Build without tests
./mvnw test                             # Run all tests
./mvnw test -Dtest=ClassName            # Run specific test class
```

### Frontend (from `/frontend`)
```bash
npm install                             # Install dependencies
npm run dev                             # Dev server (port 5173)
npm run build                           # Production build
npm run lint                            # ESLint fix
```

### Database
MariaDB required (not MySQL). Dev credentials in `backend/src/main/resources/application.properties`.

## Architecture

```
backend/src/main/java/com/maisonedition/
├── controller/     # REST endpoints (Auth, Livre, Chapitre, Commande, etc.)
├── service/        # Business logic
├── repository/     # JPA data access
├── entity/         # Database models (Livre, Chapitre, Utilisateur, Commande)
├── dto/            # API transfer objects
├── config/         # Security, CORS, initialization
└── security/       # JWT authentication

frontend/src/
├── views/
│   ├── public/     # Public pages (catalog, blog, events)
│   └── admin/      # Admin dashboard (protected)
├── components/     # Reusable Vue components
├── services/       # Axios API client
├── stores/         # Pinia state (auth)
├── router/         # Vue Router with guards
└── types/          # TypeScript interfaces
```

## Key Patterns

- **Backend:** Controller-Service-Repository layers, JWT auth, DTOs for API responses
- **Frontend:** Vue 3 Composition API, Pinia stores, route guards for admin pages
- **Path alias:** `@/*` maps to `frontend/src/*` in TypeScript/Vite
- **API proxy:** Vite proxies `/api` and `/uploads` to backend on port 8080

## Important Technical Details

- **Roles:** ADMIN, EDITEUR, UTILISATEUR (checked in route guards and backend security)
- **Payments:** Stripe integration with multiple book types (Paper, Digital, Subscriptions)
- **Rich text:** TipTap editor with KaTeX for mathematical formulas
- **PDF chapters:** pdf.js for chapter reading, with free/paid access control
- **RTL support:** Arabic fonts (Amiri, Tajawal, Noto Naskh Arabic) configured in Tailwind

## API Documentation

Swagger UI available at `http://localhost:8080/swagger-ui.html` when backend is running.

## Deployment

Production target: dar-adloun.com (Debian 13 VPS). See `/deployment/` folder for scripts and `GUIDE-DEPLOIEMENT.md` for full instructions.
