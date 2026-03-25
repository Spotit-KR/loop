package kr.io.team.loop.common.domain.event

import kr.io.team.loop.common.domain.GoalId

data class GoalDeletedEvent(
    val goalId: GoalId,
)
