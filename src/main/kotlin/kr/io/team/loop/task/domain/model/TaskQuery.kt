package kr.io.team.loop.task.domain.model

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId

data class TaskQuery(
    val memberId: MemberId? = null,
    val goalId: GoalId? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)
