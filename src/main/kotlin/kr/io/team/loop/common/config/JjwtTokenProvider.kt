package kr.io.team.loop.common.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JjwtTokenProvider(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms}") private val expirationMs: Long,
) : JwtTokenProvider {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    override fun generateToken(memberId: Long): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)
        return Jwts
            .builder()
            .subject(memberId.toString())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }
}
