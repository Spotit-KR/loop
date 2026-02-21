package kr.io.team.loop.task.application.service

import kr.io.team.loop.task.application.dto.TaskDto
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.repository.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateTaskService(
    private val taskRepository: TaskRepository,
) {
    @Transactional
    fun execute(command: TaskCommand.Create): TaskDto {
        val task = taskRepository.save(command)
        return TaskDto.from(task)
    }
}
