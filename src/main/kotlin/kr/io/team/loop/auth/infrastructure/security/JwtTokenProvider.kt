package kr.io.team.loop.auth.infrastructure.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.io.team.loop.auth.domain.model.AccessToken
import kr.io.team.loop.auth.domain.model.RefreshToken
import kr.io.team.loop.auth.domain.service.TokenProvider
import kr.io.team.loop.common.domain.MemberId
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
) : TokenProvider {
    private val signingKey: SecretKey by lazy {
        val decodedBytes = Base64.getDecoder().decode(jwtProperties.secret)
        Keys.hmacShaKeyFor(decodedBytes)
    }

    override fun generateAccessToken(memberId: MemberId): AccessToken {
        val now = Date()
        val expiration = Date(now.time + jwtProperties.accessTokenExpiry)
        val token =
            Jwts
                .builder()
                .subject(memberId.value.toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact()
        return AccessToken(token)
    }

    override fun generateRefreshToken(): RefreshToken = RefreshToken(UUID.randomUUID().toString())

    override fun getRefreshTokenExpiresAt(): LocalDateTime =
        LocalDateTime.now().plusSeconds(jwtProperties.refreshTokenExpiry / 1000)

    fun getMemberIdFromToken(token: String): Long {
        val claims =
            Jwts
                .parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
        return claims.subject.toLong()
    }

    fun isValidToken(token: String): Boolean = runCatching { getMemberIdFromToken(token) }.isSuccess
}
