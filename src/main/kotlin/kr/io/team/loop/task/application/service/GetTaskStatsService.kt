package kr.io.team.loop.task.application.service

import kr.io.team.loop.task.application.dto.TaskStatsDto
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.repository.TaskReadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetTaskStatsService(
    private val taskReadRepository: TaskReadRepository,
) {
    @Transactional(readOnly = true)
    fun execute(query: TaskQuery): TaskStatsDto {
        val tasksWithInfo = taskReadRepository.findAllWithCategoryInfo(query)
        val total = tasksWithInfo.size
        val completed = tasksWithInfo.count { it.completed }
        val rate = if (total == 0) 0.0 else (completed.toDouble() / total * 100)
        return TaskStatsDto(
            total = total,
            completed = completed,
            rate = rate,
        )
    }
}
