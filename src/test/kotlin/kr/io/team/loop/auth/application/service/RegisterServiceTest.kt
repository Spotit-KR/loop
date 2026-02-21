package kr.io.team.loop.auth.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kr.io.team.loop.auth.domain.model.AccessToken
import kr.io.team.loop.auth.domain.model.AuthCommand
import kr.io.team.loop.auth.domain.model.AuthMember
import kr.io.team.loop.auth.domain.model.AuthMemberCommand
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
import kr.io.team.loop.common.domain.Nickname
import kr.io.team.loop.common.exception.DuplicateEmailException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

class RegisterServiceTest :
    BehaviorSpec({
        val authMemberRepository = mockk<AuthMemberRepository>()
        val refreshTokenRepository = mockk<RefreshTokenRepository>()
        val tokenProvider = mockk<TokenProvider>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val service = RegisterService(authMemberRepository, refreshTokenRepository, tokenProvider, passwordEncoder)

        given("이메일 중복 없는 유효한 커맨드") {
            val command =
                AuthCommand.Register(
                    email = Email("new@example.com"),
                    password = Password("password123"),
                    nickname = Nickname("newuser"),
                )
            val memberId = MemberId(1L)
            val hashedPwd = "hashed_password"
            val authMember = AuthMember(id = memberId, email = command.email, hashedPassword = hashedPwd)
            val accessToken = AccessToken("access.token.value")
            val refreshToken = RefreshToken("refresh-token-value")
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
                every { authMemberRepository.findAll(AuthMemberQuery(email = command.email)) } returns emptyList()
                every { passwordEncoder.encode(command.password.value) } returns hashedPwd
                every {
                    authMemberRepository.save(
                        AuthMemberCommand.Create(command.email, hashedPwd, command.nickname),
                    )
                } returns authMember
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

        given("이미 존재하는 이메일") {
            val command =
                AuthCommand.Register(
                    email = Email("existing@example.com"),
                    password = Password("password123"),
                    nickname = Nickname("user"),
                )
            val existingMember = AuthMember(id = MemberId(2L), email = command.email, hashedPassword = "hashed")

            `when`("execute 호출") {
                every { authMemberRepository.findAll(AuthMemberQuery(email = command.email)) } returns
                    listOf(existingMember)

                then("DuplicateEmailException 발생") {
                    shouldThrow<DuplicateEmailException> {
                        service.execute(command)
                    }
                }
            }
        }
    })
