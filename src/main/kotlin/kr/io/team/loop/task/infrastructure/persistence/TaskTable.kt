package kr.io.team.loop.task.infrastructure.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object TaskTable : Table("task") {
    val taskId = long("task_id").autoIncrement()
    val title = text("title")
    val status = text("status")
    val goalId = long("goal_id").index()
    val memberId = long("member_id").index()
    val taskDate = date("task_date")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(taskId)
}
