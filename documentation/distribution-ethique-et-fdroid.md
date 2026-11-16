# Kit de distribution éthique (F-Droid, IzzyOnDroid et annuaires libres)

Ce document regroupe la fiche technique prête à copier-coller pour référencer **Respire** (`fr.respire.app`) sur F-Droid et IzzyOnDroid.

---

## 1. Métadonnées F-Droid (`metadata/fr.respire.app.yml`)

```yaml
Categories:
  - Time
  - System
License: MPL-2.0
AuthorName: Pilmil Studio
AuthorEmail: pilmil.studio@proton.me
WebSite: https://pilmil-studio.github.io/respire/
SourceCode: https://github.com/Pilmil-Studio/respire
IssueTracker: https://github.com/Pilmil-Studio/respire/issues
Changelog: https://github.com/Pilmil-Studio/respire/releases

AutoName: Respire

RepoType: git
Repo: https://github.com/Pilmil-Studio/respire.git

Builds:
  - versionName: 4.6.0
    versionCode: 40600
    commit: v4.6.0
    subdir: app
    gradle:
      - yes
    prebuild: sed -i -e 's/versionCode = .*/versionCode = 40600/' -e 's/versionName = .*/versionName = "4.6.0"/' build.gradle.kts

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 4.6.0
CurrentVersionCode: 40600
```

---

## 2. Référencement IzzyOnDroid / AlternativeTo / Framalibre

- **IzzyOnDroid** : soumettre `https://github.com/Pilmil-Studio/respire` sur `https://gitlab.com/IzzyOnDroid/repo/-/issues` (structure Fastlane en six langues `fr-FR`, `en-US`, `es-ES`, `de-DE`, `pl-PL`, `uk` déjà présente, 0 pisteur, 0 permission `INTERNET`).
- **AlternativeTo** : référencer dans les catégories *Screen Time*, *Digital Wellbeing* et *App Blocker* avec les critères libres (`Open Source`, `Free`, `Privacy-focused`).
