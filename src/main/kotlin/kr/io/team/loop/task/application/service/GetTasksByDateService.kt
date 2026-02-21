package kr.io.team.loop.task.application.service

import kr.io.team.loop.task.application.dto.TaskDto
import kr.io.team.loop.task.application.dto.TasksByDateDto
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.repository.TaskReadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetTasksByDateService(
    private val taskReadRepository: TaskReadRepository,
) {
    @Transactional(readOnly = true)
    fun execute(query: TaskQuery): TasksByDateDto {
        val tasksWithInfo = taskReadRepository.findAllWithCategoryInfo(query)
        val categories =
            tasksWithInfo
                .groupBy { it.categoryId }
                .map { (_, tasksInCategory) ->
                    val first = tasksInCategory.first()
                    TasksByDateDto.CategoryWithTasksDto(
                        categoryId = first.categoryId.value,
                        categoryName = first.categoryName,
                        categoryColor = first.categoryColor,
                        tasks =
                            tasksInCategory.map { taskInfo ->
                                TaskDto(
                                    id = taskInfo.id,
                                    memberId = taskInfo.memberId,
                                    categoryId = taskInfo.categoryId,
                                    title = taskInfo.title,
                                    completed = taskInfo.completed,
                                    taskDate = taskInfo.taskDate,
                                    createdAt = taskInfo.createdAt,
                                    updatedAt = taskInfo.updatedAt,
                                )
                            },
                    )
                }
        return TasksByDateDto(
            date = requireNotNull(query.taskDate) { "taskDate must not be null" }.value,
            categories = categories,
        )
    }
}
