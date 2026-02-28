package kr.io.team.loop.common.config

interface JwtTokenProvider {
    fun generateToken(memberId: Long): String

    fun validateToken(token: String): Boolean

    fun getMemberIdFromToken(token: String): Long
}
