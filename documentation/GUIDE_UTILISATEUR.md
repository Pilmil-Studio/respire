# Guide utilisateur — Respire

Respire est un outil de régulation attentionnelle et de blocage des formats vidéo courts (Shorts, Reels), conçu sans accès réseau et sans suivi publicitaire.

---

## 1. Premier lancement (`OnboardingScreen`)

L'assistant d'accueil comporte **4 étapes** :
1. **Bienvenue** (`WELCOME`) : présentation du fonctionnement local (pause respiratoire avant l'ouverture d'applications choisies).
2. **Sélection des applications** (`APPS`) : choix des applications à encadrer.
3. **Divulgation d'accessibilité** (`DISCLOSURE`) : explication claire de l'usage du service d'accessibilité Android (conformité Google Play — aucune lecture de mots de passe ni de messages).
4. **Activation** (`ACTIVATION`) : guidage vers les paramètres d'accessibilité Android, incluant la procédure spécifique Android 13+ (*Paramètres restreints*).

---

## 2. Navigation principale (3 onglets)

### 1. Applis (`Applis`)
- **Indicateur d'état** : affiche si le service d'accessibilité est actif ou en attente.
- **Liste de contrôle en 3 étapes** : suivi rapide de la configuration (applications choisies, service actif, notifications).
- **Applications suivies** : liste modifiable à tout moment via `PickerScreen`.
- **Blocage des vidéos courtes** : interrupteurs dédiés par plateforme (YouTube Shorts, Instagram Reels ; l'interrupteur Facebook Reels est en test et ne bloque encore rien), avec fenêtre de diagnostic (`DiagnosticDialog`) pour vérifier la détection.

### 2. Statistiques (`Statistiques`)
- **Historique sur 8 semaines** (`StatsCard`) balayable horizontalement.
- **Graphique en barres sur 7 jours** : ouvertures évitées vs ouvertures poursuivies.
- **Repères d'usage** : heure de pointe des tentations, estimation du temps économisé (en minutes) et compteur de vidéos courtes bloquées.
- **Export texte** : copie ou export local du résumé statistique.

### 3. Réglages (`Réglages`)
- **Règles nocturnes** (`RiskCard`) : renforcement de la friction sur une plage horaire sensible (ex. nuit).
- **Règles d'usage** (`RulesSection`) :
  - **Attente** : durée de la pause respiratoire initiale.
  - **Allongement** : augmentation progressive de l'attente au fil des ouvertures répétées.
  - **Session** : durée maximale autorisée avant nouvelle pause.
  - **Quota** : plafond journalier d'ouvertures ou de temps.
  - *Anti-rechute* : assouplir ou lever une règle impose un compte à rebours de **30 secondes** (`LOOSEN_DELAY_S`).
- **Suite Pilmil & À propos** : liens vers les autres applications du studio et informations de licence.

---

## 3. L'écran de pause (`PauseScreen` / `PauseActivity`)

Lorsqu'une application encadrée est lancée, `PauseActivity` s'interpose avant son affichage :
1. **Respiration guidée** (`BreathingCircles`) : animation visuelle uniquement (cycle 4 s inspiration / 6 s expiration, sans audio ni vibration sur Android pour rester discret).
2. **Intention** : puces rapides permettant de qualifier pourquoi vous ouvrez l'application.
3. **Décision** :
   - **« Non, je ferme »** : retour immédiat à l'écran d'accueil, comptabilisé comme ouverture évitée.
   - **« Ouvrir quand même »** : disponible uniquement à l'issue du compte à rebours configuré.
