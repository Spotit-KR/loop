package kr.io.team.loop.task.domain.model

import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.TaskId
import java.time.LocalDateTime

data class Task(
    val id: TaskId,
    val memberId: MemberId,
    val categoryId: CategoryId,
    val title: TaskTitle,
    val completed: Boolean,
    val taskDate: TaskDate,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
