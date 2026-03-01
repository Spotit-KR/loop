package kr.io.team.loop.goal.domain.model

import kr.io.team.loop.common.domain.MemberId

data class GoalQuery(
    val memberId: MemberId? = null,
)
