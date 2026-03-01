package kr.io.team.loop.goal.domain.model

import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId

sealed interface GoalCommand {
    data class Create(
        val title: GoalTitle,
        val memberId: MemberId,
    ) : GoalCommand

    data class Update(
        val goalId: GoalId,
        val title: GoalTitle,
    ) : GoalCommand

    data class Delete(
        val goalId: GoalId,
    ) : GoalCommand
}
