package kr.io.team.loop.task.infrastructure.persistence

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toLocalDateTime
import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import kr.io.team.loop.common.infrastructure.persistence.TasksTable
import kr.io.team.loop.task.domain.model.Task
import kr.io.team.loop.task.domain.model.TaskCommand
import kr.io.team.loop.task.domain.model.TaskDate
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.model.TaskTitle
import kr.io.team.loop.task.domain.repository.TaskRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedTaskRepository : TaskRepository {
    override fun save(command: TaskCommand): Task =
        when (command) {
            is TaskCommand.Create -> insert(command)
            is TaskCommand.Update -> update(command)
            is TaskCommand.ToggleComplete -> toggleComplete(command)
            is TaskCommand.Delete -> throw IllegalArgumentException("Use delete() for delete commands")
        }

    private fun insert(command: TaskCommand.Create): Task {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val id =
            TasksTable.insertAndGetId {
                it[memberId] = command.memberId.value
                it[categoryId] = command.categoryId.value
                it[title] = command.title.value
                it[completed] = false
                it[taskDate] = command.taskDate.value.toKotlinLocalDate()
                it[createdAt] = now
                it[updatedAt] = now
            }
        return findById(TaskId(id.value))
            ?: throw IllegalStateException("Failed to retrieve inserted task")
    }

    private fun update(command: TaskCommand.Update): Task {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val taskEntityId = EntityID(command.taskId.value, TasksTable)
        TasksTable.update(where = { TasksTable.id eq taskEntityId }) {
            it[title] = command.title.value
            it[taskDate] = command.taskDate.value.toKotlinLocalDate()
            it[updatedAt] = now
        }
        return findById(command.taskId)
            ?: throw NoSuchElementException("Task not found: ${command.taskId.value}")
    }

    private fun toggleComplete(command: TaskCommand.ToggleComplete): Task {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val taskEntityId = EntityID(command.taskId.value, TasksTable)
        val currentTask =
            findById(command.taskId)
                ?: throw NoSuchElementException("Task not found: ${command.taskId.value}")
        TasksTable.update(where = { TasksTable.id eq taskEntityId }) {
            it[completed] = !currentTask.completed
            it[updatedAt] = now
        }
        return findById(command.taskId)
            ?: throw NoSuchElementException("Task not found: ${command.taskId.value}")
    }

    private fun findById(id: TaskId): Task? {
        val entityId = EntityID(id.value, TasksTable)
        return TasksTable
            .selectAll()
            .where { TasksTable.id eq entityId }
            .singleOrNull()
            ?.toTask()
    }

    override fun findAll(query: TaskQuery): List<Task> {
        var dbQuery = TasksTable.selectAll()
        query.taskId?.let { taskId ->
            val entityId = EntityID(taskId.value, TasksTable)
            dbQuery = dbQuery.andWhere { TasksTable.id eq entityId }
        }
        query.memberId?.let { dbQuery = dbQuery.andWhere { TasksTable.memberId eq it.value } }
        query.taskDate?.let { dbQuery = dbQuery.andWhere { TasksTable.taskDate eq it.value.toKotlinLocalDate() } }
        return dbQuery.map { it.toTask() }
    }

    override fun delete(command: TaskCommand.Delete): Task {
        val task =
            findById(command.taskId)
                ?: throw NoSuchElementException("Task not found: ${command.taskId.value}")
        val taskEntityId = EntityID(command.taskId.value, TasksTable)
        TasksTable.deleteWhere { TasksTable.id eq taskEntityId }
        return task
    }

    private fun ResultRow.toTask(): Task =
        Task(
            id = TaskId(this[TasksTable.id].value),
            memberId = MemberId(this[TasksTable.memberId]),
            categoryId = CategoryId(this[TasksTable.categoryId]),
            title = TaskTitle(this[TasksTable.title]),
            completed = this[TasksTable.completed],
            taskDate = TaskDate(this[TasksTable.taskDate].toJavaLocalDate()),
            createdAt = this[TasksTable.createdAt].toJavaLocalDateTime(),
            updatedAt = this[TasksTable.updatedAt].toJavaLocalDateTime(),
        )
}
