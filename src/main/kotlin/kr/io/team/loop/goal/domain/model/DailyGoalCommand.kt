package kr.io.team.loop.goal.domain.model

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId

sealed interface DailyGoalCommand {
    data class Add(
        val goalId: GoalId,
        val memberId: MemberId,
        val date: LocalDate,
    ) : DailyGoalCommand

    data class Remove(
        val goalId: GoalId,
        val memberId: MemberId,
        val date: LocalDate,
    ) : DailyGoalCommand
}
