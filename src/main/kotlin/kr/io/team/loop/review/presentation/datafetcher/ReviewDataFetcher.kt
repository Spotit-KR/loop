package kr.io.team.loop.review.presentation.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kr.io.team.loop.codegen.types.CreateReviewInput
import kr.io.team.loop.codegen.types.ReviewFilter
import kr.io.team.loop.codegen.types.ReviewStepOutput
import kr.io.team.loop.codegen.types.UpdateReviewInput
import kr.io.team.loop.common.config.Authorize
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.review.application.service.ReviewService
import kr.io.team.loop.review.domain.model.Review
import kr.io.team.loop.review.domain.model.ReviewCommand
import kr.io.team.loop.review.domain.model.ReviewId
import kr.io.team.loop.review.domain.model.ReviewQuery
import kr.io.team.loop.review.domain.model.ReviewStep
import kotlin.time.Clock
import kr.io.team.loop.codegen.types.Review as ReviewGraphql
import kr.io.team.loop.codegen.types.ReviewStats as ReviewStatsGraphql
import kr.io.team.loop.codegen.types.ReviewType as ReviewTypeGraphql
import kr.io.team.loop.codegen.types.StepType as StepTypeGraphql
import kr.io.team.loop.review.domain.model.ReviewType as ReviewTypeDomain
import kr.io.team.loop.review.domain.model.StepType as StepTypeDomain

@DgsComponent
class ReviewDataFetcher(
    private val reviewService: ReviewService,
) {
    @DgsQuery
    fun myReviews(
        @InputArgument filter: ReviewFilter,
        @Authorize memberId: Long,
    ): List<ReviewGraphql> {
        val query =
            ReviewQuery(
                memberId = MemberId(memberId),
                reviewType = filter.reviewType?.let { ReviewTypeDomain.valueOf(it.name) },
                stepType = filter.stepType?.let { StepTypeDomain.valueOf(it.name) },
                date = filter.date?.let { LocalDate.parse(it) },
                startDate = filter.startDate?.let { LocalDate.parse(it) },
                endDate = filter.endDate?.let { LocalDate.parse(it) },
            )
        return reviewService.findAll(query).map { it.toGraphql() }
    }

    @DgsQuery
    fun myReviewStats(
        @Authorize memberId: Long,
    ): ReviewStatsGraphql {
        val today =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        val stats = reviewService.getStats(MemberId(memberId), today)
        return ReviewStatsGraphql(
            totalCount = stats.totalCount.toInt(),
            consecutiveDays = stats.consecutiveDays,
        )
    }

    @DgsMutation
    fun createReview(
        @InputArgument input: CreateReviewInput,
        @Authorize memberId: Long,
    ): ReviewGraphql {
        val command =
            ReviewCommand.Create(
                memberId = MemberId(memberId),
                steps =
                    input.steps.map { step ->
                        ReviewStep(
                            type = StepTypeDomain.valueOf(step.type.name),
                            content = step.content,
                        )
                    },
                date = LocalDate.parse(input.date),
            )
        return reviewService.create(command).toGraphql()
    }

    @DgsMutation
    fun updateReview(
        @InputArgument input: UpdateReviewInput,
        @Authorize memberId: Long,
    ): ReviewGraphql {
        val command =
            ReviewCommand.Update(
                reviewId = ReviewId(input.id.toLong()),
                steps =
                    input.steps.map { step ->
                        ReviewStep(
                            type = StepTypeDomain.valueOf(step.type.name),
                            content = step.content,
                        )
                    },
            )
        return reviewService.update(command, MemberId(memberId)).toGraphql()
    }

    private fun Review.toGraphql(): ReviewGraphql =
        ReviewGraphql(
            id = id.value.toString(),
            reviewType = ReviewTypeGraphql.valueOf(reviewType.name),
            steps =
                steps.map { step ->
                    ReviewStepOutput(
                        type = StepTypeGraphql.valueOf(step.type.name),
                        content = step.content,
                    )
                },
            startDate = startDate.toString(),
            endDate = endDate?.toString(),
            createdAt = createdAt.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
