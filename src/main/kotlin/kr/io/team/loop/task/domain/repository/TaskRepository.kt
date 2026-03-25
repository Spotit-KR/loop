package kr.io.team.loop.task.domain.repository

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskId
import kr.io.team.loop.task.domain.model.TaskQuery

interface TaskRepository {
    fun save(command: TaskCommand.Create): Task

    fun update(command: TaskCommand.Update): Task

    fun delete(command: TaskCommand.Delete)

    fun deleteByGoalId(goalId: GoalId)

    fun deleteByGoalIdAndMemberIdAndTaskDate(
        goalId: GoalId,
        memberId: MemberId,
        taskDate: LocalDate,
    )

    fun findAll(query: TaskQuery): List<Task>

    fun findById(id: TaskId): Task?

    fun findAllByGoalIds(goalIds: Set<GoalId>): List<Task>
}
