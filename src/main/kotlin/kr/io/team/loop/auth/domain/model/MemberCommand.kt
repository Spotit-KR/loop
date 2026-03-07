package kr.io.team.loop.auth.domain.model

sealed interface MemberCommand {
    data class Register(
        val loginId: LoginId,
        val nickname: Nickname,
        val rawPassword: String,
        val encodedPassword: String? = null,
    ) : MemberCommand

    data class Login(
        val loginId: LoginId,
        val rawPassword: String,
    ) : MemberCommand
}
