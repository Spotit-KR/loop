package kr.io.team.loop.auth.presentation.response

import kr.io.team.loop.auth.application.dto.AuthTokenDto

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        fun from(dto: AuthTokenDto) =
            AuthTokenResponse(
                accessToken = dto.accessToken,
                refreshToken = dto.refreshToken,
            )
    }
}
