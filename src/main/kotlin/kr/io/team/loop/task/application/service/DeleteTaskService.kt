package kr.io.team.loop.task.application.service

import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.repository.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteTaskService(
    private val taskRepository: TaskRepository,
) {
    @Transactional
    fun execute(command: TaskCommand.Delete) {
        val task =
            taskRepository.findAll(TaskQuery(taskId = command.taskId)).firstOrNull()
                ?: throw NoSuchElementException("Task not found: ${command.taskId.value}")
        require(task.memberId == command.memberId) {
            "Task does not belong to member: ${command.memberId.value}"
        }
        taskRepository.delete(command)
    }
}
