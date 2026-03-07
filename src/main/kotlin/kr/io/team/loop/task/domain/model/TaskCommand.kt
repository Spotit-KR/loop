package kr.io.team.loop.task.domain.model

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId

sealed interface TaskCommand {
    data class Create(
        val title: TaskTitle,
        val goalId: GoalId,
        val memberId: MemberId,
        val taskDate: LocalDate,
    ) : TaskCommand

    data class Update(
        val taskId: TaskId,
        val title: TaskTitle? = null,
        val status: TaskStatus? = null,
    ) : TaskCommand

    data class Delete(
        val taskId: TaskId,
    ) : TaskCommand
}
