package kr.io.team.loop.auth.infrastructure.persistence

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kr.io.team.loop.auth.domain.model.RefreshToken
import kr.io.team.loop.auth.domain.model.RefreshTokenCommand
import kr.io.team.loop.auth.domain.model.RefreshTokenQuery
import kr.io.team.loop.auth.domain.model.StoredRefreshToken
import kr.io.team.loop.auth.domain.repository.RefreshTokenRepository
import kr.io.team.loop.common.domain.MemberId
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class RefreshTokenRepositoryImpl : RefreshTokenRepository {
    override fun save(command: RefreshTokenCommand.Save): StoredRefreshToken {
        val now =
            java.time.LocalDateTime
                .now()
                .toKotlinLocalDateTime()
        val id =
            RefreshTokensTable.insertAndGetId {
                it[RefreshTokensTable.memberId] = EntityID(command.memberId.value, RefreshTokensTable)
                it[RefreshTokensTable.token] = command.token.value
                it[RefreshTokensTable.expiresAt] = command.expiresAt.toKotlinLocalDateTime()
                it[RefreshTokensTable.createdAt] = now
            }
        return StoredRefreshToken(
            id = id.value,
            memberId = command.memberId,
            token = command.token,
            expiresAt = command.expiresAt,
            createdAt = now.toJavaLocalDateTime(),
        )
    }

    override fun findAll(query: RefreshTokenQuery): List<StoredRefreshToken> {
        var dbQuery = RefreshTokensTable.selectAll()
        query.token?.let { token ->
            dbQuery = dbQuery.andWhere { RefreshTokensTable.token eq token.value }
        }
        query.memberId?.let { memberId ->
            val entityId = EntityID(memberId.value, RefreshTokensTable)
            dbQuery = dbQuery.andWhere { RefreshTokensTable.memberId eq entityId }
        }
        return dbQuery.map { row ->
            StoredRefreshToken(
                id = row[RefreshTokensTable.id].value,
                memberId = MemberId(row[RefreshTokensTable.memberId].value),
                token = RefreshToken(row[RefreshTokensTable.token]),
                expiresAt = row[RefreshTokensTable.expiresAt].toJavaLocalDateTime(),
                createdAt = row[RefreshTokensTable.createdAt].toJavaLocalDateTime(),
            )
        }
    }

    override fun delete(command: RefreshTokenCommand.Delete): StoredRefreshToken {
        val stored =
            findAll(RefreshTokenQuery(token = command.token)).firstOrNull()
                ?: throw NoSuchElementException("Refresh token not found")
        RefreshTokensTable.deleteWhere { RefreshTokensTable.token eq command.token.value }
        return stored
    }

    override fun deleteAllByMemberId(command: RefreshTokenCommand.DeleteAllByMemberId): List<StoredRefreshToken> {
        val stored = findAll(RefreshTokenQuery(memberId = command.memberId))
        val entityId = EntityID(command.memberId.value, RefreshTokensTable)
        RefreshTokensTable.deleteWhere { RefreshTokensTable.memberId eq entityId }
        return stored
    }
}
