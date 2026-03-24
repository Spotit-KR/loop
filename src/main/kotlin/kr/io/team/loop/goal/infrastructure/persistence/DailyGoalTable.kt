package kr.io.team.loop.goal.infrastructure.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object DailyGoalTable : Table("daily_goal") {
    val dailyGoalId = long("daily_goal_id").autoIncrement()
    val goalId = long("goal_id")
    val memberId = long("member_id").index()
    val date = date("date")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(dailyGoalId)

    init {
        uniqueIndex(goalId, memberId, date)
    }
}
