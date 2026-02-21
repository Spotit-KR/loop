package kr.io.team.loop.auth.infrastructure.persistence

import kr.io.team.loop.common.infrastructure.persistence.MembersTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.datetime

object RefreshTokensTable : LongIdTable("refresh_tokens") {
    val memberId = reference("member_id", MembersTable)
    val token = varchar("token", 500).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at")
}
