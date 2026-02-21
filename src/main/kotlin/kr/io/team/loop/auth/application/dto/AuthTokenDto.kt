package kr.io.team.loop.auth.application.dto

data class AuthTokenDto(
    val accessToken: String,
    val refreshToken: String,
)
