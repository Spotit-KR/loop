package kr.io.team.loop.member.domain.model

import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.Nickname
import java.time.LocalDateTime

data class Member(
    val id: MemberId,
    val email: Email,
    val hashedPassword: HashedPassword,
    val nickname: Nickname,
    val profileImageUrl: ProfileImageUrl?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
