package kr.io.team.loop.auth.domain.model

import kr.io.team.loop.common.domain.MemberId
import java.time.Instant

data class Member(
    val id: MemberId,
    val loginId: LoginId,
    val nickname: Nickname,
    val password: String,
    val createdAt: Instant,
    val updatedAt: Instant?,
)
