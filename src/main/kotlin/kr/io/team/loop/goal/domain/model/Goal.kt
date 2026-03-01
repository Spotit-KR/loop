package kr.io.team.loop.goal.domain.model

import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import java.time.Instant

data class Goal(
    val id: GoalId,
    val title: GoalTitle,
    val memberId: MemberId,
    val createdAt: Instant,
    val updatedAt: Instant?,
) {
    fun isOwnedBy(memberId: MemberId): Boolean = this.memberId == memberId
}
