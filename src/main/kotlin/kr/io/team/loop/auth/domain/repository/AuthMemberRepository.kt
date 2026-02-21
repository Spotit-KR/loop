package kr.io.team.loop.auth.domain.repository

import kr.io.team.loop.auth.domain.model.AuthMember
import kr.io.team.loop.auth.domain.model.AuthMemberCommand
import kr.io.team.loop.auth.domain.model.AuthMemberQuery

interface AuthMemberRepository {
    fun findAll(query: AuthMemberQuery): List<AuthMember>

    fun save(command: AuthMemberCommand.Create): AuthMember
}
