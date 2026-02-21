package kr.io.team.loop.auth.domain.repository

import kr.io.team.loop.auth.domain.model.RefreshTokenCommand
import kr.io.team.loop.auth.domain.model.RefreshTokenQuery
import kr.io.team.loop.auth.domain.model.StoredRefreshToken

interface RefreshTokenRepository {
    fun save(command: RefreshTokenCommand.Save): StoredRefreshToken

    fun findAll(query: RefreshTokenQuery): List<StoredRefreshToken>

    fun delete(command: RefreshTokenCommand.Delete): StoredRefreshToken

    fun deleteAllByMemberId(command: RefreshTokenCommand.DeleteAllByMemberId): List<StoredRefreshToken>
}
