package kr.io.team.loop.member.application.service

import kr.io.team.loop.member.application.dto.MemberDto
import kr.io.team.loop.member.domain.model.MemberCommand
import kr.io.team.loop.member.domain.model.MemberQuery
import kr.io.team.loop.member.domain.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateMemberProfileService(
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun execute(command: MemberCommand.UpdateProfile): MemberDto {
        memberRepository.findAll(MemberQuery(memberId = command.memberId)).firstOrNull()
            ?: throw NoSuchElementException("Member not found: ${command.memberId.value}")
        val updated = memberRepository.save(command)
        return MemberDto.from(updated)
    }
}
