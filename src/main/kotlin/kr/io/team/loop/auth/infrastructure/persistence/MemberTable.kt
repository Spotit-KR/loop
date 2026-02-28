package kr.io.team.loop.auth.infrastructure.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object MemberTable : Table("member") {
    val memberId = long("member_id").autoIncrement()
    val nickname = text("nickname")
    val loginId = text("login_id").uniqueIndex()
    val password = text("password")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(memberId)
}
