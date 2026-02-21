package kr.io.team.loop.common.exception

class InvalidRefreshTokenException(
    message: String = "유효하지 않거나 만료된 리프레시 토큰입니다.",
) : RuntimeException(message)
