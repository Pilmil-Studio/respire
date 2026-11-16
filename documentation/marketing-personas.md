# Personas marketing : Respire

Novembre 2026. Sources : accueil du site (fr et en), README, fiche Play fr-FR, page de confidentialité. Aucun chiffre d'usage.

## 1. Celle ou celui qui ouvre une appli sans y penser

- **Qui** : une personne qui reconnaît le geste (le pouce ouvre un réseau social ou un fil avant que la décision soit prise) et veut reprendre la main sans tout interdire.
- **Situation** : elle s'aperçoit qu'elle ouvre la même appli plusieurs fois par heure, surtout le soir.
- **Objections** : « encore une appli de temps d'écran qui culpabilise » ; « je vais la désactiver au bout de deux jours » ; « il faudra payer pour les vraies limites ».
- **10 premières secondes** : une promesse simple (une pause avant l'ouverture, fermer tout de suite possible), le prix (gratuit, sans pub), un bouton de téléchargement, une capture de l'écran de pause.
- **Mot tapé** : « arrêter le doom scroll », « limiter les réseaux sociaux Android ».

## 2. Celle ou celui qui veut seulement se débarrasser des Reels et Shorts

- **Qui** : une personne qui utilise Instagram pour ses messages et YouTube pour des vidéos choisies, mais se fait happer par les vidéos courtes.
- **Situation** : supprimer l'appli n'est pas possible, la messagerie ou les vidéos longues restent utiles.
- **Objections** : « est-ce que ça bloque aussi mes messages ? » ; « ça marche vraiment dans l'appli officielle ? » ; « et si l'appli change après une mise à jour ? ».
- **10 premières secondes** : la mention « masque les Reels et les Shorts, le reste reste utilisable », la capture avec les interrupteurs.
- **Mot tapé** : « bloquer les Reels Instagram », « désactiver les Shorts YouTube Android ».

## 3. La personne méfiante sur la vie privée

- **Qui** : une personne qui lit les permissions avant d'installer, qui préfère le logiciel libre, et pour qui une appli qui demande l'accessibilité est un signal d'alerte.
- **Situation** : elle trouve l'idée utile mais se demande pourquoi une appli de pause verrait les applis qu'elle ouvre.
- **Objections** : « pourquoi l'accessibilité ? » ; « est-ce que ça lit mes messages ? » ; « où partent les données ? » ; « qui est derrière ? ».
- **10 premières secondes** : « sans permission Internet, sans compte, sans pub », licence libre, lien vers le code source et vers la politique de confidentialité.
- **Mot tapé** : « temps d'écran open source sans internet », « appli limiter téléphone sans traceur ».

# Diagnostic

## Ce que le site donnait déjà
- Persona 1 : accroche forte (« Pourquoi tu l'ouvres maintenant ? »), fonctions détaillées, captures, simulateur. Le bouton principal était pourtant un bouton Google Play désactivé, et le téléchargement était secondaire.
- Persona 2 : le filtre Reels et Shorts est décrit dans les fonctions et visible sur la capture d'accueil. Les limites du filtre (Reel reçu en message, Facebook en test, identifiants modifiables par une mise à jour) n'étaient que dans le README.
- Persona 3 : encart « Tout reste sur ton téléphone », page de confidentialité, FAQ sur l'accessibilité en deux phrases courtes. Il manquait une réponse plus complète sur ce que le service voit et ne voit pas, et l'origine associative.

## Ce qui manquait (ajouté)
- Un bouton principal unique « Télécharger l'APK » et une ligne de réassurance.
- Une section « Pour qui » et une section « Comment ça marche » en trois étapes avec captures.
- Une section « Ce que Respire ne fait pas » reprenant les limites connues du README.
- Une FAQ nourrie par les objections ci-dessus, dont l'accessibilité.
- La mention de l'association dans « Pourquoi c'est gratuit ».

## Captures d'écran
Les sept captures de chaque langue font 1080 x 2340 (vérifié dans l'en-tête PNG). Les trois captures de l'accueil portaient `width="1080" height="2340"` et un style en ligne `width:100%; max-width:300px; height:auto`, donc le ratio était déjà respecté dans cet état, sans `aspect-ratio` ni `object-fit` ni flex étirant. La déformation signalée vient du style recopié en ligne trois fois, sans règle commune, et sans protection contre un étirement par la grille (`align-items` par défaut). Le style est maintenant porté par une classe `.shot` (ratio 1080/2340, `height:auto`, `object-fit:contain`) dans `site.css`, avec `align-self:start` dans la grille de captures.
