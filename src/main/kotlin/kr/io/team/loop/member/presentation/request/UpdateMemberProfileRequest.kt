package kr.io.team.loop.member.presentation.request

data class UpdateMemberProfileRequest(
    val nickname: String,
    val profileImageUrl: String?,
)
