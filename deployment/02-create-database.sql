-- Script de création de la base de données
-- À exécuter avec: sudo mysql < 02-create-database.sql

-- Création de la base de données
CREATE DATABASE IF NOT EXISTS maison_edition CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Création de l'utilisateur
CREATE USER IF NOT EXISTS 'maison'@'localhost' IDENTIFIED BY 'MotDePasseSecurise123!';

-- Attribution des privilèges
GRANT ALL PRIVILEGES ON maison_edition.* TO 'maison'@'localhost';
FLUSH PRIVILEGES;

-- Vérification
SHOW DATABASES;
SELECT User, Host FROM mysql.user WHERE User = 'maison';
