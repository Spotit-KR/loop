package kr.io.team.loop.auth.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.io.team.loop.auth.domain.model.AccessToken
import kr.io.team.loop.auth.domain.model.AuthCommand
import kr.io.team.loop.auth.domain.model.RefreshToken
import kr.io.team.loop.auth.domain.model.RefreshTokenCommand
import kr.io.team.loop.auth.domain.model.RefreshTokenQuery
import kr.io.team.loop.auth.domain.model.StoredRefreshToken
import kr.io.team.loop.auth.domain.repository.RefreshTokenRepository
import kr.io.team.loop.auth.domain.service.TokenProvider
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.exception.InvalidRefreshTokenException
import java.time.LocalDateTime

class RefreshServiceTest :
    BehaviorSpec({
        val refreshTokenRepository = mockk<RefreshTokenRepository>()
        val tokenProvider = mockk<TokenProvider>()
        val service = RefreshService(refreshTokenRepository, tokenProvider)

        given("유효한 리프레시 토큰") {
            val command = AuthCommand.Refresh(refreshToken = RefreshToken("valid-refresh-token"))
            val memberId = MemberId(1L)
            val storedToken =
                StoredRefreshToken(
                    id = 1L,
                    memberId = memberId,
                    token = command.refreshToken,
                    expiresAt = LocalDateTime.now().plusDays(10),
                    createdAt = LocalDateTime.now().minusDays(5),
                )
            val newAccessToken = AccessToken("new.access.token")
            val newRefreshToken = RefreshToken("new-refresh-token")
            val newExpiresAt = LocalDateTime.now().plusDays(15)
            val newStoredToken =
                StoredRefreshToken(
                    id = 2L,
                    memberId = memberId,
                    token = newRefreshToken,
                    expiresAt = newExpiresAt,
                    createdAt = LocalDateTime.now(),
                )

            `when`("execute 호출") {
                every {
                    refreshTokenRepository.findAll(RefreshTokenQuery(token = command.refreshToken))
                } returns listOf(storedToken)
                every {
                    refreshTokenRepository.delete(RefreshTokenCommand.Delete(command.refreshToken))
                } returns storedToken
                every { tokenProvider.generateAccessToken(memberId) } returns newAccessToken
                every { tokenProvider.generateRefreshToken() } returns newRefreshToken
                every { tokenProvider.getRefreshTokenExpiresAt() } returns newExpiresAt
                every {
                    refreshTokenRepository.save(RefreshTokenCommand.Save(memberId, newRefreshToken, newExpiresAt))
                } returns newStoredToken

                then("새 AuthTokenDto 반환") {
                    val result = service.execute(command)
                    result.accessToken shouldBe newAccessToken.value
                    result.refreshToken shouldBe newRefreshToken.value
                    verify { refreshTokenRepository.delete(RefreshTokenCommand.Delete(command.refreshToken)) }
                }
            }
        }

        given("존재하지 않는 리프레시 토큰") {
            val command = AuthCommand.Refresh(refreshToken = RefreshToken("unknown-token"))

            `when`("execute 호출") {
                every {
                    refreshTokenRepository.findAll(RefreshTokenQuery(token = command.refreshToken))
                } returns emptyList()

                then("InvalidRefreshTokenException 발생") {
                    shouldThrow<InvalidRefreshTokenException> {
                        service.execute(command)
                    }
                }
            }
        }

        given("만료된 리프레시 토큰") {
            val command = AuthCommand.Refresh(refreshToken = RefreshToken("expired-token"))
            val expiredToken =
                StoredRefreshToken(
                    id = 2L,
                    memberId = MemberId(1L),
                    token = command.refreshToken,
                    expiresAt = LocalDateTime.now().minusDays(1),
                    createdAt = LocalDateTime.now().minusDays(16),
                )

            `when`("execute 호출") {
                every {
                    refreshTokenRepository.findAll(RefreshTokenQuery(token = command.refreshToken))
                } returns listOf(expiredToken)

                then("InvalidRefreshTokenException 발생") {
                    shouldThrow<InvalidRefreshTokenException> {
                        service.execute(command)
                    }
                }
            }
        }
    })
