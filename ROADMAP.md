# Feuille de route — Respire

> Dernière mise à jour : novembre 2026.

## 1. Fonctionnalités à venir
- **v4.x** :
  - Affinage de la détection des lecteurs plein écran (Reels / Shorts) suite aux mises à jour des applications tierces, sans jamais demander la permission `INTERNET`.
  - Intégration optionnelle de `pilmil-core` pour l'export/import chiffré des statistiques et réglages.

## 2. Aspect visuel & UX
- Suivi des captures d'écran Paparazzi (`ScreensTest`) sur l'ensemble des états (accueil, pause 4s/6s, bilan, réglages).
- Amélioration des contrastes et du guidage d'accessibilité Android (y compris l'écran « paramètres restreints » d'Android 13+).

## 3. Qualité de code & sécurité
- À faire : ajouter `ktlint` / `detekt` au projet.

## 4. Feuille de route iOS
- **État actuel** : module iOS partiel. La coque SwiftUI (`iosApp/`) affiche l'écran de cohérence cardiaque 4 s / 6 s du module `:shared`, écrit en Compose Multiplatform, avec thème sombre et rappel quotidien local facultatif. Il n'y a pas encore de blocage d'applis sur iOS : l'appli Android repose sur `AccessibilityService`, qui n'a pas d'équivalent sur iOS.
- **Étapes restantes** :
  1. Étudier une version iOS dédiée basée sur la **Screen Time API** (`FamilyControls`, `ManagedSettings`, `DeviceActivity` en Swift).
  2. Demander à Apple l'entitlement **Family Controls** (prérequis bloquant avant toute distribution).
  3. Porter les statistiques via Compose Multiplatform si l'entitlement est accordé.
