package kr.io.team.loop.auth.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kr.io.team.loop.auth.domain.model.AccessToken
import kr.io.team.loop.auth.domain.model.AuthCommand
import kr.io.team.loop.auth.domain.model.AuthMember
import kr.io.team.loop.auth.domain.model.AuthMemberQuery
import kr.io.team.loop.auth.domain.model.Password
import kr.io.team.loop.auth.domain.model.RefreshToken
import kr.io.team.loop.auth.domain.model.RefreshTokenCommand
import kr.io.team.loop.auth.domain.model.StoredRefreshToken
import kr.io.team.loop.auth.domain.repository.AuthMemberRepository
import kr.io.team.loop.auth.domain.repository.RefreshTokenRepository
import kr.io.team.loop.auth.domain.service.TokenProvider
import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.exception.InvalidCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

class LoginServiceTest :
    BehaviorSpec({
        val authMemberRepository = mockk<AuthMemberRepository>()
        val refreshTokenRepository = mockk<RefreshTokenRepository>()
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val service = LoginService(authMemberRepository, refreshTokenRepository, tokenProvider, passwordEncoder)

        given("존재하는 이메일과 올바른 비밀번호") {
            val command =
                AuthCommand.Login(
                    email = Email("test@example.com"),
                    password = Password("password123"),
                )
            val memberId = MemberId(1L)
            val authMember = AuthMember(id = memberId, email = command.email, hashedPassword = "hashed")
            val accessToken = AccessToken("access.token")
            val refreshToken = RefreshToken("refresh-token")
            val expiresAt = LocalDateTime.now().plusDays(15)
            val storedToken =
                StoredRefreshToken(
                    id = 1L,
                    memberId = memberId,
                    token = refreshToken,
                    expiresAt = expiresAt,
                    createdAt = LocalDateTime.now(),
                )

            `when`("execute 호출") {
                every { authMemberRepository.findAll(AuthMemberQuery(email = command.email)) } returns
                    listOf(authMember)
                every { passwordEncoder.matches(command.password.value, authMember.hashedPassword) } returns true
                every { tokenProvider.generateAccessToken(memberId) } returns accessToken
                every { tokenProvider.generateRefreshToken() } returns refreshToken
                every { tokenProvider.getRefreshTokenExpiresAt() } returns expiresAt
                every {
                    refreshTokenRepository.save(RefreshTokenCommand.Save(memberId, refreshToken, expiresAt))
                } returns storedToken

                then("AuthTokenDto 반환") {
                    val result = service.execute(command)
                    result.accessToken shouldBe accessToken.value
                    result.refreshToken shouldBe refreshToken.value
                }
            }
        }

        given("존재하지 않는 이메일") {
            val command =
                AuthCommand.Login(
                    email = Email("notfound@example.com"),
                    password = Password("password123"),
                )

            `when`("execute 호출") {
                every { authMemberRepository.findAll(AuthMemberQuery(email = command.email)) } returns emptyList()

                then("InvalidCredentialsException 발생") {
                    shouldThrow<InvalidCredentialsException> {
                        service.execute(command)
                    }
                }
            }
        }

        given("잘못된 비밀번호") {
            val command =
                AuthCommand.Login(
                    email = Email("test@example.com"),
                    password = Password("wrongpass1"),
                )
            val authMember = AuthMember(id = MemberId(1L), email = command.email, hashedPassword = "hashed")

            `when`("execute 호출") {
                every { authMemberRepository.findAll(AuthMemberQuery(email = command.email)) } returns
                    listOf(authMember)
                every { passwordEncoder.matches(command.password.value, authMember.hashedPassword) } returns false

                then("InvalidCredentialsException 발생") {
                    shouldThrow<InvalidCredentialsException> {
                        service.execute(command)
                    }
                }
            }
        }
    })
