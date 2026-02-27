package kr.io.team.loop.common.config

interface JwtTokenProvider {
    fun generateToken(memberId: Long): String
}
