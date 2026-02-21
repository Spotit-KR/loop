package kr.io.team.loop.auth.domain.model

import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.Nickname

sealed interface AuthCommand {
    data class Register(
        val email: Email,
        val password: Password,
        val nickname: Nickname,
    ) : AuthCommand

    data class Login(
        val email: Email,
        val password: Password,
    ) : AuthCommand

    data class Refresh(
        val refreshToken: RefreshToken,
    ) : AuthCommand

    data class Logout(
        val refreshToken: RefreshToken,
    ) : AuthCommand
}
