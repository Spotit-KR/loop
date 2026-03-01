package kr.io.team.loop.goal.infrastructure.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object GoalTable : Table("goal") {
    val goalId = long("goal_id").autoIncrement()
    val title = text("title")
    val memberId = long("member_id").index()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(goalId)
}
