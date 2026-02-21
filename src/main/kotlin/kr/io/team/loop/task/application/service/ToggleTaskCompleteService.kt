package kr.io.team.loop.task.application.service

import kr.io.team.loop.task.application.dto.TaskDto
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.repository.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ToggleTaskCompleteService(
    private val taskRepository: TaskRepository,
) {
    @Transactional
    fun execute(command: TaskCommand.ToggleComplete): TaskDto {
        val task =
            taskRepository.findAll(TaskQuery(taskId = command.taskId)).firstOrNull()
                ?: throw NoSuchElementException("Task not found: ${command.taskId.value}")
        require(task.memberId == command.memberId) {
            "Task does not belong to member: ${command.memberId.value}"
        }
        val updatedTask = taskRepository.save(command)
        return TaskDto.from(updatedTask)
    }
}
