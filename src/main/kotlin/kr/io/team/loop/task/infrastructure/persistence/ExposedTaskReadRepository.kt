package kr.io.team.loop.task.infrastructure.persistence

import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDate
import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import kr.io.team.loop.common.infrastructure.persistence.CategoriesTable
import kr.io.team.loop.common.infrastructure.persistence.TasksTable
import kr.io.team.loop.task.domain.model.TaskDate
import kr.io.team.loop.task.domain.model.TaskQuery
import kr.io.team.loop.task.domain.model.TaskTitle
import kr.io.team.loop.task.domain.model.TaskWithCategoryInfo
import kr.io.team.loop.task.domain.repository.TaskReadRepository
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedTaskReadRepository : TaskReadRepository {
    override fun findAllWithCategoryInfo(query: TaskQuery): List<TaskWithCategoryInfo> {
        var dbQuery =
            TasksTable
                .join(CategoriesTable, JoinType.INNER, TasksTable.categoryId, CategoriesTable.id)
                .selectAll()
        query.memberId?.let { dbQuery = dbQuery.andWhere { TasksTable.memberId eq it.value } }
        query.taskDate?.let { dbQuery = dbQuery.andWhere { TasksTable.taskDate eq it.value.toKotlinLocalDate() } }
        return dbQuery.map { it.toTaskWithCategoryInfo() }
    }

    private fun ResultRow.toTaskWithCategoryInfo(): TaskWithCategoryInfo =
        TaskWithCategoryInfo(
            id = TaskId(this[TasksTable.id].value),
            memberId = MemberId(this[TasksTable.memberId]),
            categoryId = CategoryId(this[TasksTable.categoryId]),
            title = TaskTitle(this[TasksTable.title]),
            completed = this[TasksTable.completed],
            taskDate = TaskDate(this[TasksTable.taskDate].toJavaLocalDate()),
            categoryName = this[CategoriesTable.name],
            categoryColor = this[CategoriesTable.color],
            createdAt = this[TasksTable.createdAt].toJavaLocalDateTime(),
            updatedAt = this[TasksTable.updatedAt].toJavaLocalDateTime(),
        )
}
