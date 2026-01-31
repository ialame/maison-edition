#!/bin/bash
# Script de déploiement pour dar-adloun.com
# À exécuter sur le VPS après avoir cloné le projet

set -e

PROJECT_DIR="/opt/maison-edition"
REPO_URL="https://github.com/VOTRE_USERNAME/maison-edition.git"

echo "=== Déploiement de dar-adloun.com ==="

# Vérifier qu'on est root ou sudo
if [ "$EUID" -ne 0 ]; then
    echo "Ce script doit être exécuté avec sudo"
    exit 1
fi

# Cloner ou mettre à jour le repo
echo ">>> Récupération du code source..."
if [ -d "$PROJECT_DIR/repo" ]; then
    cd "$PROJECT_DIR/repo"
    git pull
else
    git clone "$REPO_URL" "$PROJECT_DIR/repo"
    cd "$PROJECT_DIR/repo"
fi

# Build du backend
echo ">>> Build du backend Spring Boot..."
cd "$PROJECT_DIR/repo/backend"
./mvnw clean package -DskipTests

# Copier le JAR
echo ">>> Déploiement du backend..."
cp target/*.jar "$PROJECT_DIR/backend/maison-edition.jar"
cp ../deployment/application-production.properties "$PROJECT_DIR/backend/"

# Build du frontend
echo ">>> Build du frontend Vue.js..."
cd "$PROJECT_DIR/repo/frontend"
npm install
npm run build

# Copier les fichiers du frontend
echo ">>> Déploiement du frontend..."
rm -rf "$PROJECT_DIR/frontend/*"
cp -r dist/* "$PROJECT_DIR/frontend/"

# Permissions
echo ">>> Configuration des permissions..."
chown -R maison:maison "$PROJECT_DIR"

# Configurer Nginx si pas déjà fait
if [ ! -f "/etc/nginx/sites-available/dar-adloun.com" ]; then
    echo ">>> Configuration de Nginx..."
    cp "$PROJECT_DIR/repo/deployment/dar-adloun.com.nginx.conf" /etc/nginx/sites-available/dar-adloun.com
    ln -sf /etc/nginx/sites-available/dar-adloun.com /etc/nginx/sites-enabled/
    rm -f /etc/nginx/sites-enabled/default
    nginx -t
    systemctl reload nginx
fi

# Configurer le service systemd si pas déjà fait
if [ ! -f "/etc/systemd/system/maison-edition.service" ]; then
    echo ">>> Configuration du service systemd..."
    cp "$PROJECT_DIR/repo/deployment/maison-edition.service" /etc/systemd/system/
    systemctl daemon-reload
    systemctl enable maison-edition
fi

# Redémarrer le backend
echo ">>> Redémarrage du backend..."
systemctl restart maison-edition

# Vérifier le statut
sleep 5
systemctl status maison-edition --no-pager

echo ""
echo "=== Déploiement terminé ==="
echo ""
echo "Vérifiez le site sur: http://dar-adloun.com"
echo ""
echo "Pour obtenir un certificat SSL:"
echo "  sudo certbot --nginx -d dar-adloun.com -d www.dar-adloun.com"
