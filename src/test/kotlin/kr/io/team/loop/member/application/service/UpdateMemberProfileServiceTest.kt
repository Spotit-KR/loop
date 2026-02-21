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

class UpdateMemberProfileServiceTest :
    BehaviorSpec({
        val memberRepository = mockk<MemberRepository>()
        val service = UpdateMemberProfileService(memberRepository)

        given("존재하는 회원") {
            val command =
                MemberCommand.UpdateProfile(
                    memberId = MemberId(1L),
                    nickname = Nickname("newnickname"),
                    profileImageUrl = null,
                )
            val now = LocalDateTime.now()
            val existingMember =
                Member(
                    id = MemberId(1L),
                    email = Email("test@example.com"),
                    hashedPassword = HashedPassword("hashed_password"),
                    nickname = Nickname("oldnickname"),
                    profileImageUrl = null,
                    createdAt = now,
                    updatedAt = now,
                )
            val updatedMember = existingMember.copy(nickname = command.nickname, updatedAt = now)

            `when`("execute 호출") {
                every { memberRepository.findAll(MemberQuery(memberId = command.memberId)) } returns
                    listOf(existingMember)
                every { memberRepository.save(command) } returns updatedMember

                then("업데이트된 MemberDto 반환") {
                    val result = service.execute(command)
                    result.nickname.value shouldBe "newnickname"
                }
            }
        }

        given("존재하지 않는 memberId") {
            val command =
                MemberCommand.UpdateProfile(
                    memberId = MemberId(999L),
                    nickname = Nickname("newnickname"),
                    profileImageUrl = null,
                )

            `when`("execute 호출") {
                every { memberRepository.findAll(MemberQuery(memberId = command.memberId)) } returns emptyList()

                then("NoSuchElementException 발생") {
                    shouldThrow<NoSuchElementException> {
                        service.execute(command)
                    }
                }
            }
        }
    })
