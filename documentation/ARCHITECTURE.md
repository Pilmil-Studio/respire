# Architecture & Choix Techniques — Respire

## 1. Structure du projet et modules

- `:app` (`fr.respire.app`) : application Android autonome (`MainActivity`, `PauseActivity`, `PauseService`, `RespireWidget`, moteur de règles et persistance).
- `:shared` : module KMP contenant `CoherenceModel.kt` (cycle 4 s / 6 s) et le pont iOS (`MainViewController`, `IosNotifications`). Note : l'application Android n'utilise pas encore `:shared` pour son UI principale.
- `pilmil-core` : socle partagé (design system).
- `iosApp` : hôte iOS.

---

## 2. Manifeste, permissions et confidentialité

Dans `AndroidManifest.xml` :
- **Seule permission standard déclarée** : `POST_NOTIFICATIONS`.
- **Aucune permission `INTERNET`** : aucune donnée d'usage ne peut quitter l'appareil.
- **Aucune permission `SYSTEM_ALERT_WINDOW`** (pas de superposition intrusive) ni **`PACKAGE_USAGE_STATS`**.
- **`android:allowBackup="false"`**.
- **`<queries>`** : résolution des applications installées ayant une icône de lancement (`CATEGORY_LAUNCHER`) pour alimenter `PickerScreen`.
- **Service système** : `PauseService` (`BIND_ACCESSIBILITY_SERVICE`), uniquement activé explicitement par l'utilisateur dans les paramètres Android.

---

## 3. Choix techniques vérifiés dans le code

### Détection d'ouverture (`PauseService`)
- Écoute ciblée des événements `TYPE_WINDOW_STATE_CHANGED` pour détecter le passage au premier plan d'une application surveillée et lancer `PauseActivity`.

### Blocage des formats courts (`ShortFormBlocker`)
- Exécution isolée sur un thread dédié `HandlerThread("respire-shorts")` avec cadence d'analyse de **120 ms**.
- Détection combinant identifiants de vues connus et critère géométrique (hauteur >= 70 % de l'écran), avec **filtres négatifs** pour ne pas bloquer les directs (live) ou les messageries privées (DM).
- Double confirmation à **250 ms** d'intervalle avant déclenchement de `GLOBAL_ACTION_BACK` (plafonné à **3 retours** maximum avec délai de refroidissement de **900 ms**).

### Persistance et rétention (`SharedPreferences`)
- Stockage local dans `SharedPreferences("respire")`.
- Purge automatique des journaux quotidiens au-delà de **60 jours** (`pruneOldDays()`).
