package fr.respire.app

/**
 * Automate pur de décision pour le blocage des Reels / Shorts :
 * temporisation après « retour », confirmation en deux temps et garde-fou
 * contre les retours en boucle.
 */
internal class ShortFormGuard(
    private val diagEveryMs: Long = DEFAULT_DIAG_EVERY_MS,
    private val exploreEveryMs: Long = DEFAULT_EXPLORE_EVERY_MS,
    private val backCooldownMs: Long = DEFAULT_BACK_COOLDOWN_MS,
    private val confirmMs: Long = DEFAULT_CONFIRM_MS,
    private val maxConsecutiveBacks: Int = DEFAULT_MAX_CONSECUTIVE_BACKS,
) {
    sealed interface PreScan {
        data class Wait(val delayMs: Long) : PreScan
        data class Scan(val explore: Boolean) : PreScan
    }

    sealed interface Action {
        data object None : Action
        data class Confirm(val delayMs: Long) : Action
        data object LimitReached : Action
        data class GoBack(val showToast: Boolean, val recheckDelayMs: Long) : Action
    }

    data class PostScan(
        val saveDiagnostic: Boolean,
        val action: Action,
    )

    private var lastDiag = 0L
    private var lastExplore = 0L
    private var cooldownUntil = 0L
    private var consecutiveBacks = 0
    private var confirmed = false

    fun beforeScan(now: Long): PreScan {
        if (now < cooldownUntil) {
            return PreScan.Wait(cooldownUntil - now)
        }
        val explore = now - lastExplore > exploreEveryMs
        if (explore) lastExplore = now
        return PreScan.Scan(explore = explore)
    }

    fun afterScan(now: Long, result: ShortFormBlocker.Result): PostScan {
        val saveDiagnostic = result.blocked || result.method == "exploration" || now - lastDiag > diagEveryMs
        if (saveDiagnostic) lastDiag = now

        if (!result.blocked) {
            consecutiveBacks = 0
            confirmed = false
            return PostScan(saveDiagnostic = saveDiagnostic, action = Action.None)
        }
        if (!confirmed) {
            confirmed = true
            return PostScan(saveDiagnostic = saveDiagnostic, action = Action.Confirm(confirmMs))
        }
        confirmed = false
        if (consecutiveBacks >= maxConsecutiveBacks) {
            return PostScan(saveDiagnostic = saveDiagnostic, action = Action.LimitReached)
        }
        consecutiveBacks++
        cooldownUntil = now + backCooldownMs
        return PostScan(
            saveDiagnostic = saveDiagnostic,
            action = Action.GoBack(
                showToast = consecutiveBacks == 1,
                recheckDelayMs = backCooldownMs,
            ),
        )
    }

    companion object {
        const val DEFAULT_DIAG_EVERY_MS = 2000L
        const val DEFAULT_EXPLORE_EVERY_MS = 6000L
        const val DEFAULT_BACK_COOLDOWN_MS = 900L
        const val DEFAULT_CONFIRM_MS = 250L
        const val DEFAULT_MAX_CONSECUTIVE_BACKS = 3
    }
}
