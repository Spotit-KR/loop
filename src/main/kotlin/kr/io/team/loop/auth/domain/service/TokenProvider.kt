package kr.io.team.loop.auth.domain.service

import kr.io.team.loop.auth.domain.model.AccessToken
import kr.io.team.loop.auth.domain.model.RefreshToken
import kr.io.team.loop.common.domain.MemberId
import java.time.LocalDateTime

interface TokenProvider {
    fun generateAccessToken(memberId: MemberId): AccessToken

    fun generateRefreshToken(): RefreshToken

    fun getRefreshTokenExpiresAt(): LocalDateTime
}
