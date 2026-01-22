#!/bin/bash
# Script d'installation du serveur pour dar-adloun.com
# À exécuter sur le VPS Debian 13

set -e

echo "=== Installation du serveur pour dar-adloun.com ==="

# Mise à jour du système
echo ">>> Mise à jour du système..."
sudo apt update && sudo apt upgrade -y

# Installation des outils de base
echo ">>> Installation des outils de base..."
sudo apt install -y curl wget git unzip nginx certbot python3-certbot-nginx ufw

# Installation de Java 17
echo ">>> Installation de Java 17..."
sudo apt install -y openjdk-17-jdk
java -version

# Installation de MariaDB
echo ">>> Installation de MariaDB..."
sudo apt install -y mariadb-server mariadb-client
sudo systemctl enable mariadb
sudo systemctl start mariadb

# Sécurisation de MariaDB
echo ">>> Configuration de MariaDB..."
echo "Veuillez exécuter: sudo mysql_secure_installation"

# Installation de Node.js 20 LTS
echo ">>> Installation de Node.js 20..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v
npm -v

# Création de l'utilisateur pour l'application
echo ">>> Création de l'utilisateur 'maison'..."
sudo useradd -r -m -d /opt/maison-edition -s /bin/bash maison || true

# Création des répertoires
echo ">>> Création des répertoires..."
sudo mkdir -p /opt/maison-edition/{backend,frontend,uploads}
sudo mkdir -p /var/log/maison-edition
sudo chown -R maison:maison /opt/maison-edition
sudo chown -R maison:maison /var/log/maison-edition

# Configuration du firewall
echo ">>> Configuration du firewall..."
sudo ufw allow ssh
sudo ufw allow http
sudo ufw allow https
sudo ufw --force enable

echo ""
echo "=== Installation terminée ==="
echo ""
echo "Prochaines étapes :"
echo "1. Exécuter: sudo mysql_secure_installation"
echo "2. Créer la base de données avec: sudo mysql < 02-create-database.sql"
echo "3. Configurer Nginx avec le fichier fourni"
echo "4. Déployer l'application"
