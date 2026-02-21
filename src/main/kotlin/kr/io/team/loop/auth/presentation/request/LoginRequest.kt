package kr.io.team.loop.auth.presentation.request

data class LoginRequest(
    val email: String,
    val password: String,
)
