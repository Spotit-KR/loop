package kr.io.team.loop.member.domain.model

import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.MemberId

data class MemberQuery(
    val memberId: MemberId? = null,
    val email: Email? = null,
)
