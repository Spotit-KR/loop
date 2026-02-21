package kr.io.team.loop.member.domain.model

import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.Nickname

sealed interface MemberCommand {
    data class Register(
        val email: Email,
        val hashedPassword: HashedPassword,
        val nickname: Nickname,
    ) : MemberCommand

    data class UpdateProfile(
        val memberId: MemberId,
        val nickname: Nickname,
        val profileImageUrl: ProfileImageUrl?,
    ) : MemberCommand
}
