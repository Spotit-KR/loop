package kr.io.team.loop.member.domain.repository

import kr.io.team.loop.member.domain.model.Member
import kr.io.team.loop.member.domain.model.MemberCommand
import kr.io.team.loop.member.domain.model.MemberQuery

interface MemberRepository {
    fun save(command: MemberCommand): Member

    fun findAll(query: MemberQuery): List<Member>
}
