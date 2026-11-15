package fr.respire.app

/**
 * Détection du lecteur de Reels de Facebook par la description d'accessibilité (contentDescription)
 * de certains éléments d'interface. Les identifiants de vues de Facebook sont tous masqués
 * (« (name removed) »), ils ne servent à rien ici.
 *
 * Logique pure, indépendante d'Android : [ShortFormBlocker] convertit les nœuds de l'écran en
 * [FbNode] puis appelle [detect]. Aucun texte affiché (publications, messages) n'est lu :
 * seules des descriptions exactement égales à un libellé connu comptent.
 */
object FacebookReels {

    /** Rectangle à l'écran, en pixels (même convention que Rect : right et bottom exclus du contenu). */
    data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width get() = (right - left).coerceAtLeast(0)
        val height get() = (bottom - top).coerceAtLeast(0)
        fun contains(x: Int, y: Int) = x in left..right && y in top..bottom
    }

    /**
     * Un nœud candidat. [coverHeight] est la plus grande hauteur parmi le nœud et ses
     * ancêtres proches : un libellé posé sur un petit élément est porté par un conteneur plein écran.
     */
    data class FbNode(
        val description: String?,
        val visible: Boolean,
        val bounds: Box,
        val coverHeight: Int,
    )

    /**
     * Libellés par langue, relevés sur appareil avec Facebook en anglais. Pour une nouvelle langue,
     * ajouter une entrée ici : la logique n'a pas à changer. Toutes les langues sont essayées,
     * quelle que soit la langue du téléphone.
     */
    val labels: Map<String, List<String>> = mapOf(
        "en" to listOf("Reels tab details", "Reel details"),
    )

    /**
     * Libellés qui n'attestent le lecteur que s'ils couvrent une grande part de l'écran
     * (le reel ouvert depuis le fil, un profil ou une notification).
     */
    val needsCover: Map<String, List<String>> = mapOf(
        "en" to listOf("Reel details"),
    )

    /** Part minimale de la hauteur de l'écran pour un libellé de [needsCover] (même seuil que les identifiants). */
    const val MIN_COVER = 0.7f

    /** Un champ de saisie visible dont le centre est dans cette part basse de l'écran gêne le blocage. */
    private const val INPUT_ZONE_FROM = 0.6f

    val allLabels: List<String> = labels.values.flatten().distinct()

    private fun matchesAny(description: String, list: Collection<List<String>>): Boolean =
        list.any { l -> l.any { it.equals(description, ignoreCase = true) } }

    /** Description normalisée si c'est exactement un libellé principal connu, sinon null. */
    fun mainLabel(description: String?): String? {
        val d = description?.trim() ?: return null
        if (d.isEmpty()) return null
        return allLabels.firstOrNull { it.equals(d, ignoreCase = true) }
    }

    /**
     * Renvoie le libellé détecté, ou null. [inputs] liste les champs de saisie visibles ;
     * il n'est appelé que si un candidat a passé tous les autres tests.
     */
    fun detect(nodes: List<FbNode>, screen: Box, inputs: () -> List<Box>): String? {
        val cx = (screen.left + screen.right) / 2
        val cy = (screen.top + screen.bottom) / 2
        val minCover = screen.height * MIN_COVER
        val hit = nodes.firstNotNullOfOrNull { n ->
            val label = mainLabel(n.description) ?: return@firstNotNullOfOrNull null
            if (!n.visible || !n.bounds.contains(cx, cy)) return@firstNotNullOfOrNull null
            val cover = matchesAny(label, needsCover.values)
            if (cover && maxOf(n.coverHeight, n.bounds.height) < minCover) return@firstNotNullOfOrNull null
            label
        } ?: return null
        // Commentaire ou message en cours de frappe : on laisse faire.
        val zoneTop = screen.top + screen.height * INPUT_ZONE_FROM
        if (inputs().any { (it.top + it.bottom) / 2 >= zoneTop }) return null
        return hit
    }
}
