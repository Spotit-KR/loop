package kr.io.team.loop.auth.domain.model

import kr.io.team.loop.common.domain.MemberId

data class RefreshTokenQuery(
    val token: RefreshToken? = null,
    val memberId: MemberId? = null,
)
