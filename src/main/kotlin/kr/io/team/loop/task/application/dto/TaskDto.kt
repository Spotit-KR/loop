package kr.io.team.loop.task.application.dto

import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskDate
import kr.io.team.loop.task.domain.model.TaskTitle
import java.time.LocalDateTime

data class TaskDto(
    val id: TaskId,
    val memberId: MemberId,
    val categoryId: CategoryId,
    val title: TaskTitle,
    val completed: Boolean,
    val taskDate: TaskDate,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(task: Task) =
            TaskDto(
                id = task.id,
                memberId = task.memberId,
                categoryId = task.categoryId,
                title = task.title,
                completed = task.completed,
                taskDate = task.taskDate,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
            )
    }
}
