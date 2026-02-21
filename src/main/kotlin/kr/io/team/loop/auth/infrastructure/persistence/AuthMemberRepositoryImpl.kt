package kr.io.team.loop.auth.infrastructure.persistence

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kr.io.team.loop.auth.domain.model.AuthMember
import kr.io.team.loop.auth.domain.model.AuthMemberCommand
import kr.io.team.loop.auth.domain.model.AuthMemberQuery
import kr.io.team.loop.auth.domain.repository.AuthMemberRepository
import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.infrastructure.persistence.MembersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class AuthMemberRepositoryImpl : AuthMemberRepository {
    override fun findAll(query: AuthMemberQuery): List<AuthMember> {
        var dbQuery = MembersTable.selectAll()
        query.email?.let { email ->
            dbQuery = dbQuery.andWhere { MembersTable.email eq email.value }
        }
        return dbQuery.map { row ->
            AuthMember(
                id = MemberId(row[MembersTable.id].value),
                email = Email(row[MembersTable.email]),
                hashedPassword = row[MembersTable.password],
            )
        }
    }

    override fun save(command: AuthMemberCommand.Create): AuthMember {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val id =
            MembersTable.insertAndGetId {
                it[MembersTable.email] = command.email.value
                it[MembersTable.password] = command.hashedPassword
                it[MembersTable.nickname] = command.nickname.value
                it[MembersTable.profileImageUrl] = null
                it[MembersTable.createdAt] = now
                it[MembersTable.updatedAt] = now
            }
        return AuthMember(id = MemberId(id.value), email = command.email, hashedPassword = command.hashedPassword)
    }
}
