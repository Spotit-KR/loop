package kr.io.team.loop.review.infrastructure.persistence

import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.review.domain.model.PeriodKey
import kr.io.team.loop.review.domain.model.Review
import kr.io.team.loop.review.domain.model.ReviewCommand
import kr.io.team.loop.review.domain.model.ReviewId
import kr.io.team.loop.review.domain.model.ReviewQuery
import kr.io.team.loop.review.domain.model.ReviewStep
import kr.io.team.loop.review.domain.model.ReviewType
import kr.io.team.loop.review.domain.model.StepType
import kr.io.team.loop.review.domain.repository.ReviewRepository
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class ExposedReviewRepository : ReviewRepository {
    override fun delete(command: ReviewCommand.Delete) {
        ReviewTable.deleteWhere { reviewId eq command.reviewId.value }
    }

    override fun save(command: ReviewCommand.Create): Review {
        val now = OffsetDateTime.now()
        val periodKey = PeriodKey.daily(command.date)
        val stepsJson = command.steps.map { StepJson(type = it.type.name, content = it.content) }

        val row =
            ReviewTable.insert {
                it[reviewType] = ReviewType.DAILY.name
                it[memberId] = command.memberId.value
                it[steps] = stepsJson
                it[startDate] = command.date
                it[this.periodKey] = periodKey.value
                it[createdAt] = now
            }

        return Review(
            id = ReviewId(row[ReviewTable.reviewId]),
            reviewType = ReviewType.DAILY,
            memberId = command.memberId,
            steps = command.steps,
            startDate = command.date,
            endDate = null,
            periodKey = periodKey,
            createdAt = now.toInstant(),
            updatedAt = null,
        )
    }

    override fun update(command: ReviewCommand.Update): Review {
        val now = OffsetDateTime.now()
        val stepsJson = command.steps.map { StepJson(type = it.type.name, content = it.content) }

        ReviewTable.update({ ReviewTable.reviewId eq command.reviewId.value }) {
            it[steps] = stepsJson
            it[updatedAt] = now
        }

        return findById(command.reviewId)!!
    }

    override fun findAll(query: ReviewQuery): List<Review> {
        var condition: Op<Boolean> = Op.TRUE
        query.memberId?.let { condition = condition and (ReviewTable.memberId eq it.value) }
        query.reviewType?.let { condition = condition and (ReviewTable.reviewType eq it.name) }
        query.date?.let {
            condition = condition and (ReviewTable.startDate eq it)
        }
        query.startDate?.let { condition = condition and (ReviewTable.startDate greaterEq it) }
        query.endDate?.let { condition = condition and (ReviewTable.startDate lessEq it) }

        return ReviewTable
            .selectAll()
            .where(condition)
            .orderBy(ReviewTable.startDate, SortOrder.DESC)
            .map { it.toReview() }
    }

    override fun findById(id: ReviewId): Review? =
        ReviewTable
            .selectAll()
            .where { ReviewTable.reviewId eq id.value }
            .singleOrNull()
            ?.toReview()

    override fun countByMemberId(memberId: MemberId): Long =
        ReviewTable
            .selectAll()
            .where { ReviewTable.memberId eq memberId.value }
            .count()

    private fun ResultRow.toReview(): Review =
        Review(
            id = ReviewId(this[ReviewTable.reviewId]),
            reviewType = ReviewType.valueOf(this[ReviewTable.reviewType]),
            memberId = MemberId(this[ReviewTable.memberId]),
            steps =
                this[ReviewTable.steps].map { stepJson ->
                    ReviewStep(
                        type = StepType.valueOf(stepJson.type),
                        content = stepJson.content,
                    )
                },
            startDate = this[ReviewTable.startDate],
            endDate = this[ReviewTable.endDate],
            periodKey = PeriodKey(this[ReviewTable.periodKey]),
            createdAt = this[ReviewTable.createdAt].toInstant(),
            updatedAt = this[ReviewTable.updatedAt]?.toInstant(),
        )
}
