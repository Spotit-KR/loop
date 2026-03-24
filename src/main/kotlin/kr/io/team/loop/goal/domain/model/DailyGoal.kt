package kr.io.team.loop.goal.domain.model

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import java.time.Instant

data class DailyGoal(
    val id: DailyGoalId,
    val goalId: GoalId,
    val memberId: MemberId,
    val date: LocalDate,
    val createdAt: Instant,
) {
    fun isOwnedBy(memberId: MemberId): Boolean = this.memberId == memberId
}
