package fr.respire.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class CoherenceModelTest {
    @Test
    fun inhaleAndExhaleBoundaries() {
        val start = CoherenceTiming.snapshotAt(0L)
        assertEquals(CoherenceTiming.Phase.INHALE, start.phase)
        assertEquals(0f, start.progress)
        assertEquals(4, start.secondsRemaining)

        val midInhale = CoherenceTiming.snapshotAt(2_000L)
        assertEquals(CoherenceTiming.Phase.INHALE, midInhale.phase)
        assertEquals(0.5f, midInhale.progress)
        assertEquals(2, midInhale.secondsRemaining)

        val endInhale = CoherenceTiming.snapshotAt(3_999L)
        assertEquals(CoherenceTiming.Phase.INHALE, endInhale.phase)
        assertEquals(3_999f / CoherenceTiming.INHALE_MS, endInhale.progress)
        assertEquals(1, endInhale.secondsRemaining)

        val startExhale = CoherenceTiming.snapshotAt(4_000L)
        assertEquals(CoherenceTiming.Phase.EXHALE, startExhale.phase)
        assertEquals(0f, startExhale.progress)
        assertEquals(6, startExhale.secondsRemaining)

        val endExhale = CoherenceTiming.snapshotAt(9_999L)
        assertEquals(CoherenceTiming.Phase.EXHALE, endExhale.phase)
        assertEquals(5_999f / CoherenceTiming.EXHALE_MS, endExhale.progress)
        assertEquals(1, endExhale.secondsRemaining)

        val wrapped = CoherenceTiming.snapshotAt(10_000L)
        assertEquals(CoherenceTiming.Phase.INHALE, wrapped.phase)
        assertEquals(0f, wrapped.progress)
        assertEquals(4, wrapped.secondsRemaining)
    }

    @Test
    fun secondsRemainingCountsDownAtExactSecondBoundaries() {
        assertEquals(4, CoherenceTiming.snapshotAt(999L).secondsRemaining)
        assertEquals(3, CoherenceTiming.snapshotAt(1_000L).secondsRemaining)
        assertEquals(3, CoherenceTiming.snapshotAt(1_001L).secondsRemaining)
        assertEquals(1, CoherenceTiming.snapshotAt(3_000L).secondsRemaining)

        assertEquals(6, CoherenceTiming.snapshotAt(4_000L).secondsRemaining)
        assertEquals(6, CoherenceTiming.snapshotAt(4_999L).secondsRemaining)
        assertEquals(5, CoherenceTiming.snapshotAt(5_000L).secondsRemaining)
        assertEquals(1, CoherenceTiming.snapshotAt(9_000L).secondsRemaining)
    }

    @Test
    fun negativeAndMultiCycleElapsedTimesWrapProperly() {
        val negOne = CoherenceTiming.snapshotAt(-1L)
        assertEquals(CoherenceTiming.Phase.EXHALE, negOne.phase)
        assertEquals(1, negOne.secondsRemaining)

        val negSixSec = CoherenceTiming.snapshotAt(-6_000L)
        assertEquals(CoherenceTiming.Phase.EXHALE, negSixSec.phase)
        assertEquals(6, negSixSec.secondsRemaining)

        val negFullCycle = CoherenceTiming.snapshotAt(-10_000L)
        assertEquals(CoherenceTiming.Phase.INHALE, negFullCycle.phase)
        assertEquals(4, negFullCycle.secondsRemaining)

        val multiCycle = CoherenceTiming.snapshotAt(25_000L)
        assertEquals(CoherenceTiming.Phase.EXHALE, multiCycle.phase)
        assertEquals(5, multiCycle.secondsRemaining)
    }
}
