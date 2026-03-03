package kr.io.team.loop.review.domain.model

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.MemberId

data class ReviewQuery(
    val memberId: MemberId? = null,
    val reviewType: ReviewType? = null,
    val stepType: StepType? = null,
    val date: LocalDate? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)
