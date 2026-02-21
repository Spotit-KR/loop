package kr.io.team.loop.member.presentation.response

import kr.io.team.loop.member.application.dto.MemberDto

data class MemberResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String?,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun from(dto: MemberDto) =
            MemberResponse(
                id = dto.id.value,
                email = dto.email.value,
                nickname = dto.nickname.value,
                profileImageUrl = dto.profileImageUrl?.value,
                createdAt = dto.createdAt.toString(),
                updatedAt = dto.updatedAt.toString(),
            )
    }
}
