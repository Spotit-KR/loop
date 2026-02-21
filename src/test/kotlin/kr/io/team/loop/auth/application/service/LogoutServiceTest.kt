package kr.io.team.loop.auth.application.service

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.io.team.loop.auth.domain.model.AuthCommand
import kr.io.team.loop.auth.domain.model.RefreshToken
import kr.io.team.loop.auth.domain.model.RefreshTokenCommand
import kr.io.team.loop.auth.domain.model.StoredRefreshToken
import kr.io.team.loop.auth.domain.repository.RefreshTokenRepository
import kr.io.team.loop.common.domain.MemberId
import java.time.LocalDateTime

class LogoutServiceTest :
    BehaviorSpec({
        val refreshTokenRepository = mockk<RefreshTokenRepository>()
        val service = LogoutService(refreshTokenRepository)

        given("유효한 로그아웃 커맨드") {
            val command = AuthCommand.Logout(refreshToken = RefreshToken("some-refresh-token"))
            val storedToken =
                StoredRefreshToken(
                    id = 1L,
                    memberId = MemberId(1L),
                    token = command.refreshToken,
                    expiresAt = LocalDateTime.now().plusDays(10),
                    createdAt = LocalDateTime.now(),
                )

            `when`("execute 호출") {
                every {
                    refreshTokenRepository.delete(RefreshTokenCommand.Delete(command.refreshToken))
                } returns storedToken

                then("토큰 삭제 호출") {
                    service.execute(command)
                    verify { refreshTokenRepository.delete(RefreshTokenCommand.Delete(command.refreshToken)) }
                }
            }
        }
    })
