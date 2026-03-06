package kr.io.team.loop.task.domain.repository

import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.TaskId
import kr.io.team.loop.task.domain.model.GoalTaskStats
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskQuery

interface TaskRepository {
    fun save(command: TaskCommand.Create): Task

    fun updateStatus(command: TaskCommand.UpdateStatus): Task

    fun delete(command: TaskCommand.Delete)

    fun findAll(query: TaskQuery): List<Task>

    fun findById(id: TaskId): Task?

    fun countByGoalIds(goalIds: Set<GoalId>): Map<GoalId, GoalTaskStats>
}
