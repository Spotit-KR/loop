package kr.io.team.loop.task.domain.model

import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId

sealed interface TaskCommand {
    data class Create(
        val memberId: MemberId,
        val categoryId: CategoryId,
        val title: TaskTitle,
        val taskDate: TaskDate,
    ) : TaskCommand

    data class Update(
        val taskId: TaskId,
        val memberId: MemberId,
        val title: TaskTitle,
        val taskDate: TaskDate,
    ) : TaskCommand

    data class ToggleComplete(
        val taskId: TaskId,
        val memberId: MemberId,
    ) : TaskCommand

    data class Delete(
        val taskId: TaskId,
        val memberId: MemberId,
    ) : TaskCommand
}
