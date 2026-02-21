package kr.io.team.loop.common.infrastructure.persistence

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.datetime

object CategoriesTable : LongIdTable("categories") {
    val memberId = long("member_id").references(MembersTable.id)
    val name = varchar("name", 50)
    val color = varchar("color", 7)
    val sortOrder = integer("sort_order")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
