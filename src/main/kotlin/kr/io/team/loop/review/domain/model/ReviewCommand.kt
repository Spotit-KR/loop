package kr.io.team.loop.review.domain.model

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.MemberId

sealed interface ReviewCommand {
    data class Create(
        val memberId: MemberId,
        val steps: List<ReviewStep>,
        val date: LocalDate,
    ) : ReviewCommand

    data class Update(
        val reviewId: ReviewId,
        val steps: List<ReviewStep>,
    ) : ReviewCommand
}
