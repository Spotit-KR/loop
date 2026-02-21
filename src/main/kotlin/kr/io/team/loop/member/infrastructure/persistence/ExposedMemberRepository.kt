package kr.io.team.loop.member.infrastructure.persistence

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.Nickname
import kr.io.team.loop.common.infrastructure.persistence.MembersTable
import kr.io.team.loop.member.domain.model.HashedPassword
import kr.io.team.loop.member.domain.model.Member
import kr.io.team.loop.member.domain.model.MemberCommand
import kr.io.team.loop.member.domain.model.MemberQuery
import kr.io.team.loop.member.domain.model.ProfileImageUrl
import kr.io.team.loop.member.domain.repository.MemberRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedMemberRepository : MemberRepository {
    override fun save(command: MemberCommand): Member =
        when (command) {
            is MemberCommand.Register -> insert(command)
            is MemberCommand.UpdateProfile -> update(command)
        }

    private fun insert(command: MemberCommand.Register): Member {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val id =
            MembersTable.insertAndGetId {
                it[email] = command.email.value
                it[password] = command.hashedPassword.value
                it[nickname] = command.nickname.value
                it[profileImageUrl] = null
                it[createdAt] = now
                it[updatedAt] = now
            }
        return findById(MemberId(id.value))
            ?: throw IllegalStateException("Failed to retrieve inserted member")
    }

    private fun update(command: MemberCommand.UpdateProfile): Member {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val memberId = EntityID(command.memberId.value, MembersTable)
        MembersTable.update(where = { MembersTable.id eq memberId }) {
            it[nickname] = command.nickname.value
            it[profileImageUrl] = command.profileImageUrl?.value
            it[updatedAt] = now
        }
        return findById(command.memberId)
            ?: throw NoSuchElementException("Member not found: ${command.memberId.value}")
    }

    private fun findById(id: MemberId): Member? {
        val entityId = EntityID(id.value, MembersTable)
        return MembersTable
            .selectAll()
            .where { MembersTable.id eq entityId }
            .singleOrNull()
            ?.toMember()
    }

    override fun findAll(query: MemberQuery): List<Member> {
        var dbQuery = MembersTable.selectAll()
        query.memberId?.let { memberId ->
            val entityId = EntityID(memberId.value, MembersTable)
            dbQuery = dbQuery.andWhere { MembersTable.id eq entityId }
        }
        query.email?.let { em ->
            dbQuery = dbQuery.andWhere { MembersTable.email eq em.value }
        }
        return dbQuery.map { it.toMember() }
    }

    private fun ResultRow.toMember(): Member =
        Member(
            id = MemberId(this[MembersTable.id].value),
            email = Email(this[MembersTable.email]),
            hashedPassword = HashedPassword(this[MembersTable.password]),
            nickname = Nickname(this[MembersTable.nickname]),
            profileImageUrl = this[MembersTable.profileImageUrl]?.let { ProfileImageUrl(it) },
            createdAt = this[MembersTable.createdAt].toJavaLocalDateTime(),
            updatedAt = this[MembersTable.updatedAt].toJavaLocalDateTime(),
        )
}
