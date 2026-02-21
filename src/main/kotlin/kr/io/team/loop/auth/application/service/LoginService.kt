package kr.io.team.loop.auth.application.service

import kr.io.team.loop.auth.application.dto.AuthTokenDto
import kr.io.team.loop.auth.domain.model.AuthCommand
import kr.io.team.loop.auth.domain.model.AuthMemberQuery
import kr.io.team.loop.auth.domain.model.RefreshTokenCommand
import kr.io.team.loop.auth.domain.repository.AuthMemberRepository
import kr.io.team.loop.auth.domain.repository.RefreshTokenRepository
import kr.io.team.loop.auth.domain.service.TokenProvider
import kr.io.team.loop.common.exception.InvalidCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoginService(
    private val authMemberRepository: AuthMemberRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenProvider: TokenProvider,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun execute(command: AuthCommand.Login): AuthTokenDto {
        val authMember =
            authMemberRepository.findAll(AuthMemberQuery(email = command.email)).firstOrNull()
                ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(command.password.value, authMember.hashedPassword)) {
            throw InvalidCredentialsException()
        }

        val accessToken = tokenProvider.generateAccessToken(authMember.id)
        val refreshToken = tokenProvider.generateRefreshToken()
        val expiresAt = tokenProvider.getRefreshTokenExpiresAt()
        refreshTokenRepository.save(RefreshTokenCommand.Save(authMember.id, refreshToken, expiresAt))

        return AuthTokenDto(accessToken = accessToken.value, refreshToken = refreshToken.value)
    }
}
