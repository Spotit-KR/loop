package kr.io.team.loop.auth.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenExpiry: Long,
    val refreshTokenExpiry: Long,
) {
    init {
        require(secret.isNotBlank()) {
            "JWT secret must not be blank. Set the JWT_SECRET environment variable."
        }
    }
}
