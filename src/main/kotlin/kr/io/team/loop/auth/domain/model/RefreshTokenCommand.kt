package kr.io.team.loop.auth.domain.model

import kr.io.team.loop.common.domain.MemberId
import java.time.LocalDateTime

sealed interface RefreshTokenCommand {
    data class Save(
        val memberId: MemberId,
        val token: RefreshToken,
        val expiresAt: LocalDateTime,
    ) : RefreshTokenCommand

    data class Delete(
        val token: RefreshToken,
    ) : RefreshTokenCommand

    data class DeleteAllByMemberId(
        val memberId: MemberId,
    ) : RefreshTokenCommand
}
