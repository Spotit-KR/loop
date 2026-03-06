package kr.io.team.loop.task.application.service

import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.exception.AccessDeniedException
import kr.io.team.loop.common.domain.exception.EntityNotFoundException
import kr.io.team.loop.task.application.dto.GoalTaskStatsDto
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.model.TaskStatus
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

    @Transactional(readOnly = true)
    fun getStatsByGoalIds(goalIds: Set<GoalId>): Map<GoalId, GoalTaskStatsDto> {
        val tasks = taskRepository.findAllByGoalIds(goalIds)
        return tasks
            .groupBy { it.goalId }
            .map { (goalId, goalTasks) ->
                goalId to
                    GoalTaskStatsDto(
                        goalId = goalId,
                        totalCount = goalTasks.size,
                        completedCount = goalTasks.count { it.status == TaskStatus.DONE },
                    )
            }.toMap()
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
