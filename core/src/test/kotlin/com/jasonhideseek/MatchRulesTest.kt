package com.jasonhideseek

import com.jasonhideseek.entities.SurvivorStatus.ALIVE
import com.jasonhideseek.entities.SurvivorStatus.ELIMINATED
import com.jasonhideseek.entities.SurvivorStatus.ESCAPED
import com.jasonhideseek.systems.MatchResult
import com.jasonhideseek.systems.MatchRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchRulesTest {
    @Test fun playerEscapeDoesNotEndRound() =
        assertNull(MatchRules.evaluate(ESCAPED, 0, 3, false))

    @Test fun playerEscapedResultWhenAllOthersDone() =
        assertEquals(MatchResult.PLAYER_ESCAPED, MatchRules.evaluate(ESCAPED, 2, 0, false))

    @Test fun playerEscapedResultWhenTimeUp() =
        assertEquals(MatchResult.PLAYER_ESCAPED, MatchRules.evaluate(ESCAPED, 1, 2, true))

    @Test fun playerEliminatedMatchContinues() =
        assertNull(MatchRules.evaluate(ELIMINATED, 0, 3, false))

    @Test fun botEscapedAfterPlayerDiedIsSurvivorsEscaped() =
        assertEquals(MatchResult.SURVIVORS_ESCAPED, MatchRules.evaluate(ELIMINATED, 2, 0, false))

    @Test fun everyoneEliminatedJasonWins() =
        assertEquals(MatchResult.JASON_WINS, MatchRules.evaluate(ELIMINATED, 0, 0, false))

    @Test fun timeUpWithoutEscapes() =
        assertEquals(MatchResult.TIMES_UP, MatchRules.evaluate(ALIVE, 0, 4, true))

    @Test fun timeUpAfterPlayerDiedButBotEscaped() =
        assertEquals(MatchResult.SURVIVORS_ESCAPED, MatchRules.evaluate(ELIMINATED, 1, 3, true))

    @Test fun nothingHappensWhileAliveAndTimeLeft() =
        assertNull(MatchRules.evaluate(ALIVE, 0, 4, false))

    @Test fun roundContinuesAfterBotEscapesSoOthersCanFollow() =
        assertNull(MatchRules.evaluate(ALIVE, 1, 4, false))
}
