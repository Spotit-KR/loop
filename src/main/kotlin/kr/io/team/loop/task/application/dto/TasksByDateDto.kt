package kr.io.team.loop.task.application.dto

import java.time.LocalDate

data class TasksByDateDto(
    val date: LocalDate,
    val categories: List<CategoryWithTasksDto>,
) {
    data class CategoryWithTasksDto(
        val categoryId: Long,
        val categoryName: String,
        val categoryColor: String,
        val tasks: List<TaskDto>,
    )
}
