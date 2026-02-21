package kr.io.team.loop.task.presentation.response

import kr.io.team.loop.task.application.dto.TasksByDateDto

data class TasksByDateResponse(
    val date: String,
    val categories: List<CategoryWithTasksResponse>,
) {
    data class CategoryWithTasksResponse(
        val categoryId: Long,
        val categoryName: String,
        val categoryColor: String,
        val tasks: List<TaskInCategoryResponse>,
    )

    data class TaskInCategoryResponse(
        val id: Long,
        val title: String,
        val completed: Boolean,
        val taskDate: String,
    )

    companion object {
        fun from(dto: TasksByDateDto) =
            TasksByDateResponse(
                date = dto.date.toString(),
                categories =
                    dto.categories.map { categoryDto ->
                        CategoryWithTasksResponse(
                            categoryId = categoryDto.categoryId,
                            categoryName = categoryDto.categoryName,
                            categoryColor = categoryDto.categoryColor,
                            tasks =
                                categoryDto.tasks.map { taskDto ->
                                    TaskInCategoryResponse(
                                        id = taskDto.id.value,
                                        title = taskDto.title.value,
                                        completed = taskDto.completed,
                                        taskDate = taskDto.taskDate.value.toString(),
                                    )
                                },
                        )
                    },
            )
    }
}
