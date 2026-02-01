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

### Configuration locale

Créer le fichier `backend/src/main/resources/application-local.properties` (gitignored) :

```properties
spring.datasource.url=jdbc:mariadb://localhost:3307/maison_edition
spring.datasource.username=maison
spring.datasource.password=VOTRE_MOT_DE_PASSE

app.jwt.secret=VOTRE_CLE_JWT

stripe.api.key=sk_test_XXXXX
stripe.webhook.secret=whsec_XXXXX

app.frontend.url=http://localhost:5173
app.cors.allowed-origins=http://localhost:5173
```

### Lancer le backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
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

## Déploiement sur le VPS

### 1. Pousser les modifications sur GitHub

```bash
git add .
git commit -m "Description des modifications"
git push
```

### 2. Se connecter au VPS et déployer

```bash
# Connexion SSH
ssh debian@dar-adloun.com

# Aller dans le répertoire du projet
cd /opt/maison-edition

# Récupérer les dernières modifications
git pull

# Reconstruire le backend
cd backend
./mvnw clean package -DskipTests

# Redémarrer le service backend
sudo systemctl restart maison-edition

# Reconstruire le frontend
cd ../frontend
npm install
npm run build

# Vérifier que tout fonctionne
sudo systemctl status maison-edition
```

### Commandes utiles sur le VPS

```bash
# Voir les logs du backend
sudo journalctl -u maison-edition -f

# Redémarrer Nginx
sudo systemctl restart nginx

# Vérifier l'état des services
sudo systemctl status maison-edition
sudo systemctl status nginx
sudo systemctl status mariadb
```

## Tarification

| Option | Prix | Durée |
|--------|------|-------|
| Livre papier | Variable | Permanent |
| Téléchargement PDF | 10€ | Permanent |
| Lecture d'un livre | 5€ | 1 an |
| Abonnement mensuel (tous les livres) | 30€ | 1 mois |
| Abonnement annuel (tous les livres) | 50€ | 1 an |
