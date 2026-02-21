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
import kr.io.team.loop.member.domain.model.MemberQuery
import kr.io.team.loop.member.domain.repository.MemberRepository
import java.time.LocalDateTime

class GetMemberServiceTest :
    BehaviorSpec({
        val memberRepository = mockk<MemberRepository>()
        val service = GetMemberService(memberRepository)

        given("존재하는 memberId") {
            val memberId = MemberId(1L)
            val query = MemberQuery(memberId = memberId)
            val now = LocalDateTime.now()
            val member =
                Member(
                    id = memberId,
                    email = Email("test@example.com"),
                    hashedPassword = HashedPassword("hashed_password"),
                    nickname = Nickname("testuser"),
                    profileImageUrl = null,
                    createdAt = now,
                    updatedAt = now,
                )

            `when`("execute 호출") {
                every { memberRepository.findAll(query) } returns listOf(member)

                then("MemberDto 반환") {
                    val result = service.execute(query)
                    result.id.value shouldBe 1L
                    result.email.value shouldBe "test@example.com"
                }
            }
        }

        given("존재하지 않는 memberId") {
            val memberId = MemberId(999L)
            val query = MemberQuery(memberId = memberId)

            `when`("execute 호출") {
                every { memberRepository.findAll(query) } returns emptyList()

                then("NoSuchElementException 발생") {
                    shouldThrow<NoSuchElementException> {
                        service.execute(query)
                    }
                }
            }
        }
    })
