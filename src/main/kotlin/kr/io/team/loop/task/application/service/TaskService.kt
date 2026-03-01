package kr.io.team.loop.task.application.service

import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.exception.AccessDeniedException
import kr.io.team.loop.common.domain.exception.EntityNotFoundException
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.repository.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskService(
    private val taskRepository: TaskRepository,
) {
    @Transactional
    fun create(command: TaskCommand.Create): Task = taskRepository.save(command)

    @Transactional(readOnly = true)
    fun findAll(query: TaskQuery): List<Task> = taskRepository.findAll(query)

    @Transactional
    fun updateStatus(
        command: TaskCommand.UpdateStatus,
        memberId: MemberId,
    ): Task {
        val task =
            taskRepository.findById(command.taskId)
                ?: throw EntityNotFoundException("Task not found: ${command.taskId.value}")
        if (!task.isOwnedBy(memberId)) {
            throw AccessDeniedException("Task does not belong to member: ${memberId.value}")
        }
        return taskRepository.updateStatus(command)
    }

    @Transactional
    fun delete(
        command: TaskCommand.Delete,
        memberId: MemberId,
    ) {
        val task =
            taskRepository.findById(command.taskId)
                ?: throw EntityNotFoundException("Task not found: ${command.taskId.value}")
        if (!task.isOwnedBy(memberId)) {
            throw AccessDeniedException("Task does not belong to member: ${memberId.value}")
        }
        taskRepository.delete(command)
    }
}
