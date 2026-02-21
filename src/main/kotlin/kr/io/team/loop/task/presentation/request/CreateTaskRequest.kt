package kr.io.team.loop.task.presentation.request

data class CreateTaskRequest(
    val categoryId: Long,
    val title: String,
    val taskDate: String,
)
