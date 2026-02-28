package kr.io.team.loop.auth.domain.model

sealed interface MemberCommand {
    data class Register(
        val loginId: LoginId,
        val nickname: Nickname,
        val rawPassword: String,
    ) : MemberCommand
}
