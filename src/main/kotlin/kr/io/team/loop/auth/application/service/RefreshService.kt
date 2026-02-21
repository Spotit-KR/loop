package kr.io.team.loop.auth.application.service

import kr.io.team.loop.auth.application.dto.AuthTokenDto
import kr.io.team.loop.auth.domain.model.AuthCommand
import kr.io.team.loop.auth.domain.model.RefreshTokenCommand
import kr.io.team.loop.auth.domain.model.RefreshTokenQuery
import kr.io.team.loop.auth.domain.repository.RefreshTokenRepository
import kr.io.team.loop.auth.domain.service.TokenProvider
import kr.io.team.loop.common.exception.InvalidRefreshTokenException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RefreshService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenProvider: TokenProvider,
) {
    @Transactional
    fun execute(command: AuthCommand.Refresh): AuthTokenDto {
        val storedToken =
            refreshTokenRepository.findAll(RefreshTokenQuery(token = command.refreshToken)).firstOrNull()
                ?: throw InvalidRefreshTokenException()

        if (storedToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw InvalidRefreshTokenException()
        }

        refreshTokenRepository.delete(RefreshTokenCommand.Delete(command.refreshToken))

        val newAccessToken = tokenProvider.generateAccessToken(storedToken.memberId)
        val newRefreshToken = tokenProvider.generateRefreshToken()
        val newExpiresAt = tokenProvider.getRefreshTokenExpiresAt()
        refreshTokenRepository.save(RefreshTokenCommand.Save(storedToken.memberId, newRefreshToken, newExpiresAt))

        return AuthTokenDto(accessToken = newAccessToken.value, refreshToken = newRefreshToken.value)
    }
}
