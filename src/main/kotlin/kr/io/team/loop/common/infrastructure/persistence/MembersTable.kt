package kr.io.team.loop.common.infrastructure.persistence

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.datetime

object MembersTable : LongIdTable("members") {
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255)
    val nickname = varchar("nickname", 50)
    val profileImageUrl = varchar("profile_image_url", 500).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
