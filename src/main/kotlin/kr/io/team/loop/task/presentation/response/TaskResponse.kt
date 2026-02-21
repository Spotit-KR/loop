package kr.io.team.loop.task.presentation.response

import kr.io.team.loop.task.application.dto.TaskDto

data class TaskResponse(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val completed: Boolean,
    val taskDate: String,
) {
    companion object {
        fun from(dto: TaskDto) =
            TaskResponse(
                id = dto.id.value,
                categoryId = dto.categoryId.value,
                title = dto.title.value,
                completed = dto.completed,
                taskDate = dto.taskDate.value.toString(),
            )
    }
}
