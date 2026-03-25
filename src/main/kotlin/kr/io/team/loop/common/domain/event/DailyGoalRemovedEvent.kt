package kr.io.team.loop.common.domain.event

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId

data class DailyGoalRemovedEvent(
    val goalId: GoalId,
    val date: LocalDate,
)
