package kr.io.team.loop.member.presentation.controller

import kr.io.team.loop.common.config.CurrentMemberId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.Nickname
import kr.io.team.loop.member.application.service.GetMemberService
import kr.io.team.loop.member.application.service.UpdateMemberProfileService
import kr.io.team.loop.member.domain.model.MemberCommand
import kr.io.team.loop.member.domain.model.MemberQuery
import kr.io.team.loop.member.domain.model.ProfileImageUrl
import kr.io.team.loop.member.presentation.request.UpdateMemberProfileRequest
import kr.io.team.loop.member.presentation.response.MemberResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/members")
class MemberController(
    private val getMemberService: GetMemberService,
    private val updateMemberProfileService: UpdateMemberProfileService,
) {
    @GetMapping("/me")
    fun getMe(
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<MemberResponse> {
        val dto = getMemberService.execute(MemberQuery(memberId = MemberId(memberId)))
        return ResponseEntity.ok(MemberResponse.from(dto))
    }

    @PutMapping("/me")
    fun updateMe(
        @RequestBody request: UpdateMemberProfileRequest,
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<MemberResponse> {
        val command =
            MemberCommand.UpdateProfile(
                memberId = MemberId(memberId),
                nickname = Nickname(request.nickname),
                profileImageUrl = request.profileImageUrl?.let { ProfileImageUrl(it) },
            )
        val dto = updateMemberProfileService.execute(command)
        return ResponseEntity.ok(MemberResponse.from(dto))
    }
}
