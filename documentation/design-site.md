# Diagnostic du site (novembre 2026)

Constat avant reprise :
1. Les nouvelles sections s'enchaînent sans rythme : toutes les listes sont du texte nu, sans carte, alors que la page compte maintenant dix sections.
2. Les titres h2 étaient limités à 22 caractères de large, ce qui coupait les titres longs sur quatre ou cinq lignes.
3. Deux paragraphes `.intro` consécutifs (gratuité) étaient séparés par 40 px, comme un titre et son texte.
4. La FAQ gardait le triangle natif et une zone de clic de 30 px de haut, sous les 44 px demandés.
5. Le simulateur ajouté en ligne utilisait un vert hors palette (#3d6b52) et des boutons de 36 px.
6. En-tête mobile : cinq liens sur une ligne débordent à 360 px.
7. Aucun contraste à corriger : encre 11,3, texte atténué 5,3, lien 6,7, blanc sur sauge 5,3 (AA partout).
8. Pas de thème sombre défini dans l'appli : non ajouté.

## Captures « comprimées » : cause

Les 14 PNG font tous 1080 x 2340 et les attributs width/height des `<img>` sont corrects.
Aucune règle globale `img { ... }` n'existe, aucun conteneur ne fixe de hauteur. La règle `.shot`
(commit af39004) avait déjà `height:auto` et `aspect-ratio: 1080 / 2340` conformes au fichier, donc
dans l'état du dépôt je ne reproduis pas de déformation par les règles lues. Points fragiles
corrigés quand même : `aspect-ratio` en dur dans le CSS (faux dès qu'une capture change de
format), `width:100%` avec `border` sous `box-sizing:border-box` (le ratio s'applique à la boîte
bordure, pas au bitmap), `object-fit:contain` qui masque l'écart au lieu de le signaler, `align-self`
inopérant sur un enfant de `li` non flex. Le ratio vient désormais uniquement des attributs
`width`/`height`, le cadre est un `box-shadow` (aucun effet sur la boîte). Si l'auteur voit encore
des captures écrasées, vérifier d'abord un cache navigateur ou CDN servant l'ancien `site.css`
(les anciennes règles de la grille pouvaient étirer) et un zoom de texte forcé.
