package kr.io.team.loop.auth.application.service

import kr.io.team.loop.auth.application.dto.AuthTokenDto
import kr.io.team.loop.auth.domain.model.AuthCommand
import kr.io.team.loop.auth.domain.model.AuthMemberCommand
import kr.io.team.loop.auth.domain.model.AuthMemberQuery
import kr.io.team.loop.auth.domain.model.RefreshTokenCommand
import kr.io.team.loop.auth.domain.repository.AuthMemberRepository
import kr.io.team.loop.auth.domain.repository.RefreshTokenRepository
import kr.io.team.loop.auth.domain.service.TokenProvider
import kr.io.team.loop.common.exception.DuplicateEmailException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterService(
    private val authMemberRepository: AuthMemberRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenProvider: TokenProvider,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun execute(command: AuthCommand.Register): AuthTokenDto {
        if (authMemberRepository.findAll(AuthMemberQuery(email = command.email)).isNotEmpty()) {
            throw DuplicateEmailException()
        }
        val hashedPassword =
            passwordEncoder.encode(command.password.value)
                ?: throw IllegalStateException("Password encoding failed")
        val authMember =
            authMemberRepository.save(
                AuthMemberCommand.Create(
                    email = command.email,
                    hashedPassword = hashedPassword,
                    nickname = command.nickname,
                ),
            )

        val accessToken = tokenProvider.generateAccessToken(authMember.id)
        val refreshToken = tokenProvider.generateRefreshToken()
        val expiresAt = tokenProvider.getRefreshTokenExpiresAt()
        refreshTokenRepository.save(RefreshTokenCommand.Save(authMember.id, refreshToken, expiresAt))

        return AuthTokenDto(accessToken = accessToken.value, refreshToken = refreshToken.value)
    }
}
