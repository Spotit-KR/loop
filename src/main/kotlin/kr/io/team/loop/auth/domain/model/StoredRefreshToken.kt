package kr.io.team.loop.auth.domain.model

import kr.io.team.loop.common.domain.MemberId
import java.time.LocalDateTime

data class StoredRefreshToken(
    val id: Long,
    val memberId: MemberId,
    val token: RefreshToken,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
)
