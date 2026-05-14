# 🎓 SkillPath - Advanced Learning Platform

SkillPath est une plateforme d'apprentissage desktop entièrement construite en **Java 17+ / JavaFX 21**, avec MySQL comme base de données relationnelle. Elle intègre un BackOffice analytique, un système de sécurité avancé et un bridge IA Flask pour la détection d'intrusion.

---

## 🚀 Fonctionnalités Principales

### 🖥️ BackOffice (JavaFX)

- **Tableau de Bord Analytique** : Visualisation en temps réel des statistiques utilisateurs, cours et performances.
- **Gestion des Utilisateurs** : Annuaire complet avec gestion des rôles (Admin, Architecte, Étudiant) et vérification des comptes.
- **Système de Cours & Modules** : CRUD complet pour la structure pédagogique.
- **Gestion des Événements** : Planification et analytiques des événements liés à la formation.
- **Shield Center (Sécurité)** :
  - Surveillance des logs de connexion.
  - Analyse d'intrusion (Bridge avec une IA Flask).
  - Blocage automatique d'IP suspectes.
  - Journalisation des événements de sécurité.

---

## 🛠️ Stack Technique

| Technologie | Utilisation |
| :--- | :--- |
| **Java 17+** | Application Desktop & Services de Sécurité |
| **JavaFX 21** | Interface graphique BackOffice |
| **MySQL 8.x** | Base de données relationnelle |
| **jBCrypt** | Hachage sécurisé des mots de passe |
| **Flask (Python)** | Bridge IA pour la détection d'intrusion |

---

## ⚙️ Configuration & Installation

### Prérequis

- Java Development Kit (JDK) 17 ou supérieur.
- Serveur MySQL (XAMPP ou Docker).
- Maven.

### Installation

1. Configurez votre fichier `.env` à la racine avec vos accès DB :
   ```env
   DB_URL=jdbc:mysql://localhost:3306/skillpathdb
   DB_USER=root
   DB_PWD=
   ```
2. Importez les dépendances via Maven.
3. Lancez la classe `Launcher.java`.

---

## 🛡️ Sécurité & Rôles

- **ROLE_ADMIN** : Accès complet au BackOffice JavaFX et au Dashboard analytique.
- **ROLE_USER** : Accès limité aux fonctionnalités utilisateur.

Les mots de passe sont hachés via **jBCrypt** au format `$2a$`, nativement compatible Java.

---

## 👥 Contributeurs

Développé dans le cadre du projet **3A52 - Esprit PIDEV**.

---

*© 2026 SkillPath - All Rights Reserved.*
