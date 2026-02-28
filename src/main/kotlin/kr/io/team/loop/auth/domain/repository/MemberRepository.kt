package kr.io.team.loop.auth.domain.repository

import kr.io.team.loop.auth.domain.model.LoginId
import kr.io.team.loop.auth.domain.model.Member
import kr.io.team.loop.auth.domain.model.MemberCommand

interface MemberRepository {
    fun save(
        command: MemberCommand.Register,
        encodedPassword: String,
    ): Member

    fun findByLoginId(loginId: LoginId): Member?

    fun existsByLoginId(loginId: LoginId): Boolean
}
