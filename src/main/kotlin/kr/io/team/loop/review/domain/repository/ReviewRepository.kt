package kr.io.team.loop.review.domain.repository

import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.review.domain.model.Review
import kr.io.team.loop.review.domain.model.ReviewCommand
import kr.io.team.loop.review.domain.model.ReviewQuery

interface ReviewRepository {
    fun save(command: ReviewCommand.Create): Review

    fun update(command: ReviewCommand.Update): Review

    fun delete(command: ReviewCommand.Delete)

    fun findAll(query: ReviewQuery): List<Review>

    fun findById(id: kr.io.team.loop.review.domain.model.ReviewId): Review?

    fun countByMemberId(memberId: MemberId): Long
}
