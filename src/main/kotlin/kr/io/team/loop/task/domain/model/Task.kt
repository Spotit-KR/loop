package kr.io.team.loop.task.domain.model

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import java.time.Instant

data class Task(
    val id: TaskId,
    val title: TaskTitle,
    val status: TaskStatus,
    val goalId: GoalId,
    val memberId: MemberId,
    val taskDate: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant?,
) {
    fun isOwnedBy(memberId: MemberId): Boolean = this.memberId == memberId
}
