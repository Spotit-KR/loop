package kr.io.team.loop.member.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.Nickname
import kr.io.team.loop.member.domain.model.HashedPassword
import kr.io.team.loop.member.domain.model.Member
import kr.io.team.loop.member.domain.model.MemberCommand
import kr.io.team.loop.member.domain.model.MemberQuery
import kr.io.team.loop.member.domain.repository.MemberRepository
import java.time.LocalDateTime

class RegisterMemberServiceTest :
    BehaviorSpec({
        val memberRepository = mockk<MemberRepository>()
        val service = RegisterMemberService(memberRepository)

        given("유효한 커맨드") {
            val command =
                MemberCommand.Register(
                    email = Email("test@example.com"),
                    hashedPassword = HashedPassword("hashed_password"),
                    nickname = Nickname("testuser"),
                )
            val now = LocalDateTime.now()
            val member =
                Member(
                    id = MemberId(1L),
                    email = command.email,
                    hashedPassword = command.hashedPassword,
                    nickname = command.nickname,
                    profileImageUrl = null,
                    createdAt = now,
                    updatedAt = now,
                )

            `when`("execute 호출") {
                every { memberRepository.findAll(MemberQuery(email = command.email)) } returns emptyList()
                every { memberRepository.save(command) } returns member

                then("MemberDto 반환") {
                    val result = service.execute(command)
                    result.id.value shouldBe 1L
                    result.email.value shouldBe "test@example.com"
                    result.nickname.value shouldBe "testuser"
                }
            }
        }

        given("이미 존재하는 이메일") {
            val command =
                MemberCommand.Register(
                    email = Email("existing@example.com"),
                    hashedPassword = HashedPassword("hashed_password"),
                    nickname = Nickname("testuser"),
                )
            val now = LocalDateTime.now()
            val existingMember =
                Member(
                    id = MemberId(2L),
                    email = command.email,
                    hashedPassword = command.hashedPassword,
                    nickname = command.nickname,
                    profileImageUrl = null,
                    createdAt = now,
                    updatedAt = now,
                )

            `when`("execute 호출") {
                every { memberRepository.findAll(MemberQuery(email = command.email)) } returns listOf(existingMember)

                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        service.execute(command)
                    }
                }
            }
        }
    })
