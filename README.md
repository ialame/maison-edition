# Maison d'Édition - دار عدلون

Application web pour une maison d'édition avec gestion de livres, chapitres, commandes et abonnements.

## Stack Technique

- **Backend**: Spring Boot 3.2, MariaDB, JWT
- **Frontend**: Vue.js 3, TypeScript, Tailwind CSS
- **Paiement**: Stripe

## Développement Local

### Prérequis

- Java 17+
- Node.js 18+
- MariaDB (ou tunnel SSH vers VPS)

### Tunnel SSH vers la base de données VPS

```bash
ssh -L 3307:localhost:3306 debian@dar-adloun.com -N
```

Laisser le terminal ouvert pendant le développement.

### Synchroniser les uploads depuis le VPS

```bash
rsync -avz debian@dar-adloun.com:/opt/maison-edition/uploads/ backend/uploads/
```

### Lancer le backend

```bash
cd backend
./mvnw spring-boot:run
```

### Lancer le frontend

```bash
cd frontend
npm install
npm run dev
```

## Cartes de Test Stripe

Pour tester les paiements en mode test, utilisez ces numéros de carte :

| Numéro de carte | Description |
|-----------------|-------------|
| `4242 4242 4242 4242` | Paiement réussi |
| `4000 0000 0000 3220` | Authentification 3D Secure requise |
| `4000 0000 0000 9995` | Paiement refusé (fonds insuffisants) |
| `4000 0000 0000 0002` | Carte refusée |

**Pour toutes les cartes :**
- Date d'expiration : n'importe quelle date future (ex: 12/34)
- CVC : n'importe quel code à 3 chiffres (ex: 123)
- Code postal : n'importe quel code (ex: 12345)

## Structure du Projet

```
maison-edition/
├── backend/          # API Spring Boot
├── frontend/         # Application Vue.js
└── README.md
```
