package kr.io.team.loop.member.application.dto

import kr.io.team.loop.common.domain.Email
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.Nickname
import kr.io.team.loop.member.domain.model.Member
import kr.io.team.loop.member.domain.model.ProfileImageUrl
import java.time.LocalDateTime

data class MemberDto(
    val id: MemberId,
    val email: Email,
    val nickname: Nickname,
    val profileImageUrl: ProfileImageUrl?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(member: Member) =
            MemberDto(
                id = member.id,
                email = member.email,
                nickname = member.nickname,
                profileImageUrl = member.profileImageUrl,
                createdAt = member.createdAt,
                updatedAt = member.updatedAt,
            )
    }
}
