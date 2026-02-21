package kr.io.team.loop.auth.presentation.request

data class LogoutRequest(
    val refreshToken: String,
)
