package kr.io.team.loop.auth.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.every
import io.mockk.mockk
import kr.io.team.loop.auth.domain.model.LoginId
import kr.io.team.loop.auth.domain.model.Member
import kr.io.team.loop.auth.domain.model.MemberCommand
import kr.io.team.loop.auth.domain.model.Nickname
import kr.io.team.loop.auth.domain.repository.MemberRepository
import kr.io.team.loop.common.config.JwtTokenProvider
import kr.io.team.loop.common.domain.MemberId
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant

class AuthServiceTest :
    BehaviorSpec({

        val memberRepository = mockk<MemberRepository>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val jwtTokenProvider = mockk<JwtTokenProvider>()
        val authService = AuthService(memberRepository, passwordEncoder, jwtTokenProvider)

        val registerCommand =
            MemberCommand.Register(
                loginId = LoginId("testuser"),
                nickname = Nickname("홍길동"),
                rawPassword = "password123",
            )

        val savedMember =
            Member(
                id = MemberId(1L),
                loginId = LoginId("testuser"),
                nickname = Nickname("홍길동"),
                password = "encoded_password",
                createdAt = Instant.now(),
                updatedAt = null,
            )

        Given("회원가입 시") {
            When("유효한 정보이면") {
                every { memberRepository.existsByLoginId(registerCommand.loginId) } returns false
                every { passwordEncoder.encode("password123") } returns "encoded_password"
                every { memberRepository.save(registerCommand, "encoded_password") } returns savedMember
                every { jwtTokenProvider.generateToken(1L) } returns "jwt-token"

                val result = authService.register(registerCommand)

                Then("accessToken을 반환한다") {
                    result.accessToken.shouldNotBeBlank()
                    result.accessToken shouldBe "jwt-token"
                }
            }

            When("중복된 loginId이면") {
                every { memberRepository.existsByLoginId(registerCommand.loginId) } returns true

                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        authService.register(registerCommand)
                    }
                }
            }
        }

        Given("로그인 시") {
            When("유효한 자격 증명이면") {
                every { memberRepository.findByLoginId(LoginId("testuser")) } returns savedMember
                every { passwordEncoder.matches("password123", "encoded_password") } returns true
                every { jwtTokenProvider.generateToken(1L) } returns "jwt-token"

                val result = authService.login(LoginId("testuser"), "password123")

                Then("accessToken을 반환한다") {
                    result.accessToken.shouldNotBeBlank()
                    result.accessToken shouldBe "jwt-token"
                }
            }

            When("존재하지 않는 loginId이면") {
                every { memberRepository.findByLoginId(LoginId("unknown")) } returns null

                Then("예외가 발생한다") {
                    shouldThrow<NoSuchElementException> {
                        authService.login(LoginId("unknown"), "password123")
                    }
                }
            }

            When("비밀번호가 불일치하면") {
                every { memberRepository.findByLoginId(LoginId("testuser")) } returns savedMember
                every { passwordEncoder.matches("wrongpassword", "encoded_password") } returns false

                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        authService.login(LoginId("testuser"), "wrongpassword")
                    }
                }
            }
        }
    })
