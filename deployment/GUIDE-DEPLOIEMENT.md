# Guide de déploiement - dar-adloun.com
# دليل نشر الموقع

## Informations du serveur
- **Domaine**: dar-adloun.com
- **VPS**: vps-1b0f7959.vps.ovh.net
- **IP**: 135.125.102.34
- **OS**: Debian 13
- **Utilisateur**: debian

---

## Étape 1 : Connexion au VPS

```bash
ssh debian@135.125.102.34
```

---

## Étape 2 : Installation des prérequis

```bash
# Mise à jour du système
sudo apt update && sudo apt upgrade -y

# Installation des outils
sudo apt install -y curl wget git unzip nginx certbot python3-certbot-nginx ufw

# Installation de Java 17
sudo apt install -y openjdk-17-jdk

# Installation de MariaDB
sudo apt install -y mariadb-server mariadb-client
sudo systemctl enable mariadb
sudo systemctl start mariadb

# Sécuriser MariaDB
sudo mysql_secure_installation

# Installation de Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

---

## Étape 3 : Configuration de la base de données

```bash
sudo mysql
```

Dans MySQL :
```sql
CREATE DATABASE maison_edition CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'maison'@'localhost' IDENTIFIED BY 'MotDePasseSecurise123!';
GRANT ALL PRIVILEGES ON maison_edition.* TO 'maison'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

---

## Étape 4 : Création de l'utilisateur et des répertoires

```bash
# Créer l'utilisateur
sudo useradd -r -m -d /opt/maison-edition -s /bin/bash maison

# Créer les répertoires
sudo mkdir -p /opt/maison-edition/{backend,frontend,uploads,repo}
sudo mkdir -p /var/log/maison-edition
sudo chown -R maison:maison /opt/maison-edition
sudo chown -R maison:maison /var/log/maison-edition
```

---

## Étape 5 : Cloner le projet

```bash
cd /opt/maison-edition
sudo -u maison git clone https://github.com/VOTRE_USERNAME/maison-edition.git repo
```

---

## Étape 6 : Build et déploiement du Backend

```bash
cd /opt/maison-edition/repo/backend

# Build avec Maven
sudo -u maison ./mvnw clean package -DskipTests

# Copier le JAR
sudo cp target/*.jar /opt/maison-edition/backend/maison-edition.jar

# Copier la config de production
sudo cp ../deployment/application-production.properties /opt/maison-edition/backend/

# ⚠️ IMPORTANT: Modifier le mot de passe et la clé JWT dans le fichier
sudo nano /opt/maison-edition/backend/application-production.properties
```

---

## Étape 7 : Build et déploiement du Frontend

```bash
cd /opt/maison-edition/repo/frontend

# Installer les dépendances
sudo -u maison npm install

# Build pour la production
sudo -u maison npm run build

# Copier les fichiers
sudo cp -r dist/* /opt/maison-edition/frontend/
sudo chown -R maison:maison /opt/maison-edition/frontend
```

---

## Étape 8 : Configuration de Nginx

```bash
# Copier la configuration
sudo cp /opt/maison-edition/repo/deployment/dar-adloun.com.nginx.conf /etc/nginx/sites-available/dar-adloun.com

# Activer le site
sudo ln -sf /etc/nginx/sites-available/dar-adloun.com /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default

# Tester et recharger
sudo nginx -t
sudo systemctl reload nginx
```

---

## Étape 9 : Configuration du service systemd

```bash
# Copier le service
sudo cp /opt/maison-edition/repo/deployment/maison-edition.service /etc/systemd/system/

# Activer et démarrer
sudo systemctl daemon-reload
sudo systemctl enable maison-edition
sudo systemctl start maison-edition

# Vérifier le statut
sudo systemctl status maison-edition
```

---

## Étape 10 : Configuration du DNS chez OVH

Dans votre espace client OVH, ajoutez ces enregistrements DNS :

| Type | Nom | Valeur |
|------|-----|--------|
| A | @ | 135.125.102.34 |
| A | www | 135.125.102.34 |
| AAAA | @ | 2001:41d0:404:200::78a6 |
| AAAA | www | 2001:41d0:404:200::78a6 |

---

## Étape 11 : Certificat SSL (Let's Encrypt)

```bash
# Attendre que le DNS soit propagé (peut prendre jusqu'à 24h)
# Puis exécuter :
sudo certbot --nginx -d dar-adloun.com -d www.dar-adloun.com
```

---

## Étape 12 : Configuration du Firewall

```bash
sudo ufw allow ssh
sudo ufw allow http
sudo ufw allow https
sudo ufw enable
```

---

## Commandes utiles

### Logs
```bash
# Logs de l'application
sudo tail -f /var/log/maison-edition/app.log

# Logs Nginx
sudo tail -f /var/log/nginx/dar-adloun.access.log
sudo tail -f /var/log/nginx/dar-adloun.error.log
```

### Gestion du service
```bash
# Redémarrer
sudo systemctl restart maison-edition

# Arrêter
sudo systemctl stop maison-edition

# Statut
sudo systemctl status maison-edition
```

### Mise à jour du site
```bash
cd /opt/maison-edition/repo
sudo -u maison git pull

# Rebuild backend
cd backend
sudo -u maison ./mvnw clean package -DskipTests
sudo cp target/*.jar /opt/maison-edition/backend/maison-edition.jar
sudo systemctl restart maison-edition

# Rebuild frontend
cd ../frontend
sudo -u maison npm install
sudo -u maison npm run build
sudo cp -r dist/* /opt/maison-edition/frontend/
```

---

## Accès admin

Après le déploiement, connectez-vous à :
- **URL**: https://dar-adloun.com/login
- **Email**: admin@daralnashr.com
- **Mot de passe**: admin123

⚠️ **Changez immédiatement le mot de passe admin après la première connexion !**
