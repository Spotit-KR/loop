package kr.io.team.loop.auth.domain.model

import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.MemberId

data class AuthMember(
    val id: MemberId,
    val email: Email,
    val hashedPassword: String,
)
