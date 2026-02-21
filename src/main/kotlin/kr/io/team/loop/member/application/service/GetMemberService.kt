package kr.io.team.loop.member.application.service

import kr.io.team.loop.member.application.dto.MemberDto
import kr.io.team.loop.member.domain.model.MemberQuery
import kr.io.team.loop.member.domain.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetMemberService(
    private val memberRepository: MemberRepository,
) {
    @Transactional(readOnly = true)
    fun execute(query: MemberQuery): MemberDto {
        val member =
            memberRepository.findAll(query).firstOrNull()
                ?: throw NoSuchElementException("Member not found: ${query.memberId?.value}")
        return MemberDto.from(member)
    }
}
