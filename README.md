# 🎓 SkillPath - Advanced Learning Platform

SkillPath est une plateforme d'apprentissage innovante combinant une application desktop **JavaFX** pour l'administration (BackOffice) et une application web **Symfony** pour l'expérience utilisateur (FrontOffice). Le projet intègre des fonctionnalités de sécurité avancées et une analyse intelligente des comportements.

---

## 🚀 Fonctionnalités Principales

### 🖥️ BackOffice (JavaFX)
- **Tableau de Bord Analytique** : Visualisation en temps réel des statistiques utilisateurs, cours et performances.
*   **Gestion des Utilisateurs** : Annuaire complet avec gestion des rôles (Admin, Architecte, Étudiant) et vérification des comptes.
*   **Système de Cours & Modules** : CRUD complet pour la structure pédagogique.
*   **Gestion des Événements** : Planification et analytiques des événements liés à la formation.
*   **Shield Center (Sécurité)** :
    *   Surveillance des logs de connexion.
    *   Analyse d'intrusion (Bridge avec une IA Flask).
    *   Blocage automatique d'IP suspectes.
    *   Journalisation des événements de sécurité.

### 🌐 FrontOffice (Symfony)
*   **Interface Apprenant** : Parcours utilisateur fluide pour la consultation des cours.
*   **Authentification unifiée** : Connexion compatible avec les hashs Java et support de Google Login.
*   **Profil Personnalisé** : Gestion des préférences d'apprentissage et des domaines d'expertise.

---

## 🛠️ Stack Technique

| Technologie | Utilisation |
| :--- | :--- |
| **Java 17+** | Application Desktop & Services de Sécurité |
| **JavaFX 21** | Interface graphique BackOffice |
| **MySQL 8.x** | Base de données relationnelle |
| **jBCrypt** | Hachage sécurisé des mots de passe |
| **Flask (Python)** | Bridge IA pour la détection d'intrusion |
| **TailwindCSS** | Design système moderne pour le Web |

---

## ⚙️ Configuration & Installation

### Prérequis
- Java Development Kit (JDK) 17 ou supérieur.
- PHP 8.2+ & Composer.
- Serveur MySQL (XAMPP ou Docker).

### Installation (Java)
1. Configurez votre fichier `.env` à la racine avec vos accès DB :
   ```env
   DB_URL=jdbc:mysql://localhost:3306/skillpathdb
   DB_USER=root
   DB_PWD=
   ```
2. Importez les dépendances via Maven.
3. Lancez la classe `Launcher.java`.

### Installation (Symfony)
1. Installez les dépendances : `composer install`.
2. Configurez le fichier `.env.local` avec l'URL de la base de données.
3. Lancez le serveur : `symfony serve`.

---

## 🛡️ Sécurité & Redirection
Le projet utilise une logique de redirection intelligente :
*   **ROLE_ADMIN** : Accès au BackOffice JavaFX et au Dashboard Symfony.
*   **ROLE_USER** : Accès limité au FrontOffice.
*   **Compatibilité de Hash** : Les mots de passe sont hachés au format `$2y$` (Symfony) et convertis dynamiquement en `$2a$` pour la compatibilité Java/jBCrypt.

---

## 👥 Contributeurs
Développé dans le cadre du projet **3A52 - Esprit PIDEV**.

---
*© 2026 SkillPath - All Rights Reserved.*
