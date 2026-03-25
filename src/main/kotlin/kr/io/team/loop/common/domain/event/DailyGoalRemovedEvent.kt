package kr.io.team.loop.common.domain.event

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId

data class DailyGoalRemovedEvent(
    val goalId: GoalId,
    val memberId: MemberId,
    val date: LocalDate,
)
