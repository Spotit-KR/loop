package kr.io.team.loop.goal.domain.model

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId

data class GoalQuery(
    val memberId: MemberId? = null,
    val id: GoalId? = null,
    val ids: List<GoalId>? = null,
    val title: String? = null,
    val assignedDate: LocalDate? = null,
)
