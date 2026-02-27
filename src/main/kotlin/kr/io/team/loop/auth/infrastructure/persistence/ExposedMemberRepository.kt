package kr.io.team.loop.auth.infrastructure.persistence

import kr.io.team.loop.auth.domain.model.LoginId
import kr.io.team.loop.auth.domain.model.Member
import kr.io.team.loop.auth.domain.model.MemberCommand
import kr.io.team.loop.auth.domain.model.Nickname
import kr.io.team.loop.auth.domain.repository.MemberRepository
import kr.io.team.loop.common.domain.MemberId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class ExposedMemberRepository : MemberRepository {
    override fun save(
        command: MemberCommand.Register,
        encodedPassword: String,
    ): Member {
        val now = OffsetDateTime.now()
        val row =
            MemberTable.insert {
                it[nickname] = command.nickname.value
                it[loginId] = command.loginId.value
                it[password] = encodedPassword
                it[createdAt] = now
            }
        return Member(
            id = MemberId(row[MemberTable.memberId]),
            loginId = command.loginId,
            nickname = command.nickname,
            password = encodedPassword,
            createdAt = now.toInstant(),
            updatedAt = null,
        )
    }

    override fun findByLoginId(loginId: LoginId): Member? =
        MemberTable
            .selectAll()
            .where { MemberTable.loginId eq loginId.value }
            .singleOrNull()
            ?.toMember()

    override fun existsByLoginId(loginId: LoginId): Boolean =
        MemberTable
            .selectAll()
            .where { MemberTable.loginId eq loginId.value }
            .count() > 0

    private fun ResultRow.toMember(): Member =
        Member(
            id = MemberId(this[MemberTable.memberId]),
            loginId = LoginId(this[MemberTable.loginId]),
            nickname = Nickname(this[MemberTable.nickname]),
            password = this[MemberTable.password],
            createdAt = this[MemberTable.createdAt].toInstant(),
            updatedAt = this[MemberTable.updatedAt]?.toInstant(),
        )
}
