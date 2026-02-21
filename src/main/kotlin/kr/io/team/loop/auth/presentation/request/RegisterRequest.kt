package kr.io.team.loop.auth.presentation.request

data class RegisterRequest(
    val email: String,
    val password: String,
    val nickname: String,
)
