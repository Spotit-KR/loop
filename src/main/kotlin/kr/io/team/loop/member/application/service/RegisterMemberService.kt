package kr.io.team.loop.member.application.service

import kr.io.team.loop.member.application.dto.MemberDto
import kr.io.team.loop.member.domain.model.MemberCommand
import kr.io.team.loop.member.domain.model.MemberQuery
import kr.io.team.loop.member.domain.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterMemberService(
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun execute(command: MemberCommand.Register): MemberDto {
        require(memberRepository.findAll(MemberQuery(email = command.email)).isEmpty()) {
            "Email already exists: ${command.email.value}"
        }
        val member = memberRepository.save(command)
        return MemberDto.from(member)
    }
}
