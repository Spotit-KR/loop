package kr.io.team.loop.auth.domain.model

import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.Nickname

sealed interface AuthMemberCommand {
    data class Create(
        val email: Email,
        val hashedPassword: String,
        val nickname: Nickname,
    ) : AuthMemberCommand
}
