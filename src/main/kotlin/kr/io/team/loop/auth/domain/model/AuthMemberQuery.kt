package kr.io.team.loop.auth.domain.model

import kr.io.team.loop.common.domain.Email

data class AuthMemberQuery(
    val email: Email? = null,
)
