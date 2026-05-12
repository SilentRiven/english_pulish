package com.workplat.englishpulish.domain.fsrs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsTest {

    private val now = 1_700_000_000_000L // any fixed epoch ms
    private val dayMs = 86_400_000L

    @Test
    fun `new card with Good leaves Learning state with first review timestamp`() {
        val card = FsrsState.newCard(dueAt = now)
        val next = Fsrs.next(card, Rating.Good, now)
        assertEquals(CardState.Learning, next.state)
        assertNotNull(next.lastReviewAt)
        assertEquals(1, next.reps)
        assertEquals(0, next.lapses)
        assertTrue("dueAt should be in the future", next.dueAt > now)
    }

    @Test
    fun `new card with Again increments lapses and enters Relearning`() {
        val card = FsrsState.newCard(dueAt = now)
        val next = Fsrs.next(card, Rating.Again, now)
        assertEquals(CardState.Relearning, next.state)
        assertEquals(1, next.lapses)
        assertEquals(1, next.reps)
    }

    @Test
    fun `Easy schedules further out than Good on first review`() {
        val card = FsrsState.newCard(dueAt = now)
        val good = Fsrs.next(card, Rating.Good, now)
        val easy = Fsrs.next(card, Rating.Easy, now)
        assertTrue("Easy due > Good due", easy.dueAt > good.dueAt)
    }

    @Test
    fun `consecutive Goods grow the interval`() {
        var card = FsrsState.newCard(dueAt = now)
        card = Fsrs.next(card, Rating.Good, now)
        val firstInterval = card.dueAt - now

        // Pretend we waited until it was due, then rated Good again.
        val t2 = card.dueAt
        card = Fsrs.next(card, Rating.Good, t2)
        val secondInterval = card.dueAt - t2

        assertTrue(
            "second interval ($secondInterval) should exceed first ($firstInterval)",
            secondInterval > firstInterval,
        )
    }

    @Test
    fun `Again after several Goods drops stability and increments lapses`() {
        var card = FsrsState.newCard(dueAt = now)
        card = Fsrs.next(card, Rating.Good, now)
        val t2 = card.dueAt
        card = Fsrs.next(card, Rating.Good, t2)
        val stabilityBefore = card.stability
        val lapsesBefore = card.lapses

        val t3 = card.dueAt
        card = Fsrs.next(card, Rating.Again, t3)

        assertTrue("stability should drop on Again", card.stability < stabilityBefore)
        assertEquals(lapsesBefore + 1, card.lapses)
        assertEquals(CardState.Relearning, card.state)
    }

    @Test
    fun `currentRetrievability is 1 at 0 elapsed days and decays over time`() {
        val s = 5.0
        val r0 = Fsrs.currentRetrievability(s, 0.0)
        val r5 = Fsrs.currentRetrievability(s, 5.0)
        val r50 = Fsrs.currentRetrievability(s, 50.0)
        assertTrue("R(0) ≈ 1", r0 > 0.99)
        assertTrue("R decays", r5 > r50)
        assertTrue("R stays in (0, 1]", r50 in 0.0..1.0)
    }

    @Test
    fun `difficulty stays clamped within 1 to 10`() {
        var card = FsrsState.newCard(dueAt = now)
        // Hammer Again many times to push difficulty toward the upper bound.
        repeat(20) {
            val t = card.lastReviewAt?.plus(dayMs) ?: now
            card = Fsrs.next(card, Rating.Again, t)
            assertTrue("difficulty ${card.difficulty} ∈ [1, 10]", card.difficulty in 1.0..10.0)
        }
    }
}
