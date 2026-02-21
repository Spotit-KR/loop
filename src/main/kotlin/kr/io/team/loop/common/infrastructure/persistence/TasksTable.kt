package kr.io.team.loop.common.infrastructure.persistence

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime

object TasksTable : LongIdTable("tasks") {
    val memberId = long("member_id").references(MembersTable.id)
    val categoryId = long("category_id").references(CategoriesTable.id)
    val title = varchar("title", 200)
    val completed = bool("completed")
    val taskDate = date("task_date")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
