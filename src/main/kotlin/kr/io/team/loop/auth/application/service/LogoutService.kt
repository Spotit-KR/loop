package kr.io.team.loop.auth.application.service

import kr.io.team.loop.auth.domain.model.AuthCommand
import kr.io.team.loop.auth.domain.model.RefreshTokenCommand
import kr.io.team.loop.auth.domain.repository.RefreshTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LogoutService(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    @Transactional
    fun execute(command: AuthCommand.Logout) {
        refreshTokenRepository.delete(RefreshTokenCommand.Delete(command.refreshToken))
    }
}
