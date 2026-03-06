package kr.io.team.loop.task.infrastructure.persistence

import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.model.TaskStatus
import kr.io.team.loop.task.domain.model.TaskTitle
import kr.io.team.loop.task.domain.repository.TaskRepository
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class ExposedTaskRepository : TaskRepository {
    override fun save(command: TaskCommand.Create): Task {
        val now = OffsetDateTime.now()
        val row =
            TaskTable.insert {
                it[title] = command.title.value
                it[status] = TaskStatus.TODO.name
                it[goalId] = command.goalId.value
                it[memberId] = command.memberId.value
                it[taskDate] = command.taskDate
                it[createdAt] = now
            }
        return Task(
            id = TaskId(row[TaskTable.taskId]),
            title = command.title,
            status = TaskStatus.TODO,
            goalId = command.goalId,
            memberId = command.memberId,
            taskDate = command.taskDate,
            createdAt = now.toInstant(),
            updatedAt = null,
        )
    }

    override fun update(command: TaskCommand.Update): Task {
        val now = OffsetDateTime.now()
        TaskTable.update({ TaskTable.taskId eq command.taskId.value }) {
            command.title?.let { newTitle -> it[title] = newTitle.value }
            command.status?.let { newStatus -> it[status] = newStatus.name }
            it[updatedAt] = now
        }
        return findById(command.taskId)!!
    }

    override fun delete(command: TaskCommand.Delete) {
        TaskTable.deleteWhere { taskId eq command.taskId.value }
    }

    override fun findAll(query: TaskQuery): List<Task> {
        var condition: Op<Boolean> = Op.TRUE
        query.memberId?.let { condition = condition and (TaskTable.memberId eq it.value) }
        query.goalId?.let { condition = condition and (TaskTable.goalId eq it.value) }
        query.startDate?.let { condition = condition and (TaskTable.taskDate greaterEq it) }
        query.endDate?.let { condition = condition and (TaskTable.taskDate lessEq it) }
        return TaskTable
            .selectAll()
            .where(condition)
            .orderBy(TaskTable.taskDate, SortOrder.ASC)
            .map { it.toTask() }
    }

    override fun findById(id: TaskId): Task? =
        TaskTable
            .selectAll()
            .where { TaskTable.taskId eq id.value }
            .singleOrNull()
            ?.toTask()

    private fun ResultRow.toTask(): Task =
        Task(
            id = TaskId(this[TaskTable.taskId]),
            title = TaskTitle(this[TaskTable.title]),
            status = TaskStatus.valueOf(this[TaskTable.status]),
            goalId = GoalId(this[TaskTable.goalId]),
            memberId = MemberId(this[TaskTable.memberId]),
            taskDate = this[TaskTable.taskDate],
            createdAt = this[TaskTable.createdAt].toInstant(),
            updatedAt = this[TaskTable.updatedAt]?.toInstant(),
        )
}
