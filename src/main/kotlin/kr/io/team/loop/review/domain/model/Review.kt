package kr.io.team.loop.review.domain.model

import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.MemberId
import java.time.Instant

data class Review(
    val id: ReviewId,
    val reviewType: ReviewType,
    val memberId: MemberId,
    val steps: List<ReviewStep>,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val periodKey: PeriodKey,
    val createdAt: Instant,
    val updatedAt: Instant?,
) {
    fun isOwnedBy(memberId: MemberId): Boolean = this.memberId == memberId

    fun containsStepType(stepType: StepType): Boolean = steps.any { it.type == stepType }

    fun withUpdatedSteps(newSteps: List<ReviewStep>): Review = copy(steps = newSteps)
}
