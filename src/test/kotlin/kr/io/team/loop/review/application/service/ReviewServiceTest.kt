package kr.io.team.loop.review.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.common.domain.exception.AccessDeniedException
import kr.io.team.loop.common.domain.exception.DuplicateEntityException
import kr.io.team.loop.common.domain.exception.EntityNotFoundException
import kr.io.team.loop.review.domain.model.PeriodKey
import kr.io.team.loop.review.domain.model.Review
import kr.io.team.loop.review.domain.model.ReviewCommand
import kr.io.team.loop.review.domain.model.ReviewId
import kr.io.team.loop.review.domain.model.ReviewQuery
import kr.io.team.loop.review.domain.model.ReviewStep
import kr.io.team.loop.review.domain.model.ReviewType
import kr.io.team.loop.review.domain.model.StepType
import kr.io.team.loop.review.domain.repository.ReviewRepository
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant

class ReviewServiceTest :
    BehaviorSpec({

        val reviewRepository = mockk<ReviewRepository>()
        val reviewService = ReviewService(reviewRepository)

        val memberId = MemberId(1L)
        val today = LocalDate(2026, 2, 20)

        val savedReview =
            Review(
                id = ReviewId(1L),
                reviewType = ReviewType.DAILY,
                memberId = memberId,
                steps =
                    listOf(
                        ReviewStep(type = StepType.KEEP, content = "코드 리뷰 잘 했다"),
                        ReviewStep(type = StepType.PROBLEM, content = "늦게 일어났다"),
                        ReviewStep(type = StepType.TRY, content = "일찍 자기"),
                    ),
                startDate = today,
                endDate = null,
                periodKey = PeriodKey.daily(today),
                createdAt = Instant.now(),
                updatedAt = null,
            )

        Given("회고 생성 시") {
            When("유효한 입력이면") {
                val command =
                    ReviewCommand.Create(
                        memberId = memberId,
                        steps =
                            listOf(
                                ReviewStep(type = StepType.KEEP, content = "코드 리뷰 잘 했다"),
                                ReviewStep(type = StepType.PROBLEM, content = "늦게 일어났다"),
                                ReviewStep(type = StepType.TRY, content = "일찍 자기"),
                            ),
                        date = today,
                    )
                every { reviewRepository.save(command) } returns savedReview

                val result = reviewService.create(command)

                Then("생성된 회고를 반환한다") {
                    result.id.value shouldBe 1L
                    result.reviewType shouldBe ReviewType.DAILY
                    result.steps shouldHaveSize 3
                    result.memberId shouldBe memberId
                }
            }

            When("같은 날짜에 이미 회고가 존재하면") {
                val command =
                    ReviewCommand.Create(
                        memberId = memberId,
                        steps = listOf(ReviewStep(type = StepType.KEEP, content = "좋은 점")),
                        date = today,
                    )
                every { reviewRepository.save(command) } throws
                    DataIntegrityViolationException("Unique constraint violated")

                Then("DuplicateEntityException이 발생한다") {
                    shouldThrow<DuplicateEntityException> {
                        reviewService.create(command)
                    }
                }
            }
        }

        Given("회고 수정 시") {
            val updateCommand =
                ReviewCommand.Update(
                    reviewId = ReviewId(1L),
                    steps =
                        listOf(
                            ReviewStep(type = StepType.KEEP, content = "수정된 좋은 점"),
                            ReviewStep(type = StepType.TRY, content = "수정된 다짐"),
                        ),
                )

            When("본인 회고를 수정하면") {
                every { reviewRepository.findById(ReviewId(1L)) } returns savedReview
                val updatedReview = savedReview.withUpdatedSteps(updateCommand.steps)
                every { reviewRepository.update(updateCommand) } returns updatedReview

                val result = reviewService.update(updateCommand, memberId)

                Then("수정된 회고를 반환한다") {
                    result.steps shouldHaveSize 2
                    result.steps[0].content shouldBe "수정된 좋은 점"
                    result.steps[1].content shouldBe "수정된 다짐"
                }
            }

            When("존재하지 않는 회고를 수정하면") {
                every { reviewRepository.findById(ReviewId(1L)) } returns null

                Then("EntityNotFoundException이 발생한다") {
                    shouldThrow<EntityNotFoundException> {
                        reviewService.update(updateCommand, memberId)
                    }
                }
            }

            When("다른 사용자의 회고를 수정하면") {
                every { reviewRepository.findById(ReviewId(1L)) } returns savedReview
                val otherMemberId = MemberId(99L)

                Then("AccessDeniedException이 발생한다") {
                    shouldThrow<AccessDeniedException> {
                        reviewService.update(updateCommand, otherMemberId)
                    }
                }
            }
        }

        Given("회고 목록 조회 시") {
            When("해당 사용자의 회고가 있으면") {
                val query = ReviewQuery(memberId = memberId)
                every { reviewRepository.findAll(query) } returns listOf(savedReview)

                val result = reviewService.findAll(query)

                Then("회고 목록을 반환한다") {
                    result shouldHaveSize 1
                    result[0].reviewType shouldBe ReviewType.DAILY
                }
            }

            When("stepType 필터가 있으면 앱 레벨에서 필터링한다") {
                val reviewWithoutTry =
                    savedReview.copy(
                        id = ReviewId(2L),
                        steps = listOf(ReviewStep(type = StepType.KEEP, content = "좋은 점")),
                    )
                val query = ReviewQuery(memberId = memberId, stepType = StepType.TRY)
                // Repository는 stepType 없이 조회 (DB 레벨 필터만)
                val dbQuery = query.copy(stepType = null)
                every { reviewRepository.findAll(dbQuery) } returns listOf(savedReview, reviewWithoutTry)

                val result = reviewService.findAll(query)

                Then("해당 stepType을 포함하는 회고만 반환한다") {
                    result shouldHaveSize 1
                    result[0].id.value shouldBe 1L
                }
            }

            When("회고가 없으면") {
                val query = ReviewQuery(memberId = MemberId(99L))
                every { reviewRepository.findAll(query) } returns emptyList()

                val result = reviewService.findAll(query)

                Then("빈 목록을 반환한다") {
                    result shouldHaveSize 0
                }
            }
        }

        Given("회고 통계 조회 시") {
            When("회고가 있으면") {
                every { reviewRepository.countByMemberId(memberId) } returns 5L
                val reviewDates =
                    listOf(
                        LocalDate(2026, 2, 20),
                        LocalDate(2026, 2, 19),
                        LocalDate(2026, 2, 18),
                    )
                val reviews =
                    reviewDates.mapIndexed { index, date ->
                        savedReview.copy(
                            id = ReviewId((index + 1).toLong()),
                            startDate = date,
                            periodKey = PeriodKey.daily(date),
                        )
                    }
                val query = ReviewQuery(memberId = memberId)
                every { reviewRepository.findAll(query) } returns reviews

                val result = reviewService.getStats(memberId, today)

                Then("총 회고 수와 연속 회고일수를 반환한다") {
                    result.totalCount shouldBe 5L
                    result.consecutiveDays shouldBe 3
                }
            }

            When("회고가 없으면") {
                every { reviewRepository.countByMemberId(memberId) } returns 0L
                val query = ReviewQuery(memberId = memberId)
                every { reviewRepository.findAll(query) } returns emptyList()

                val result = reviewService.getStats(memberId, today)

                Then("0을 반환한다") {
                    result.totalCount shouldBe 0L
                    result.consecutiveDays shouldBe 0
                }
            }

            When("연속이 끊긴 경우") {
                every { reviewRepository.countByMemberId(memberId) } returns 3L
                val reviews =
                    listOf(
                        savedReview.copy(
                            id = ReviewId(1L),
                            startDate = LocalDate(2026, 2, 20),
                            periodKey = PeriodKey.daily(LocalDate(2026, 2, 20)),
                        ),
                        // 2026-02-19 없음 (연속 끊김)
                        savedReview.copy(
                            id = ReviewId(2L),
                            startDate = LocalDate(2026, 2, 18),
                            periodKey = PeriodKey.daily(LocalDate(2026, 2, 18)),
                        ),
                        savedReview.copy(
                            id = ReviewId(3L),
                            startDate = LocalDate(2026, 2, 17),
                            periodKey = PeriodKey.daily(LocalDate(2026, 2, 17)),
                        ),
                    )
                val query = ReviewQuery(memberId = memberId)
                every { reviewRepository.findAll(query) } returns reviews

                val result = reviewService.getStats(memberId, today)

                Then("연속 회고일수는 today부터 연속된 일수만 카운트한다") {
                    result.totalCount shouldBe 3L
                    result.consecutiveDays shouldBe 1
                }
            }
        }

        Given("회고 삭제 시") {
            val reviewId = ReviewId(1L)
            val command = ReviewCommand.Delete(reviewId = reviewId)

            When("본인 회고이면") {
                every { reviewRepository.findById(reviewId) } returns savedReview
                justRun { reviewRepository.delete(command) }

                reviewService.delete(command, memberId)

                Then("삭제가 수행된다") {
                    verify { reviewRepository.delete(command) }
                }
            }

            When("회고가 존재하지 않으면") {
                every { reviewRepository.findById(reviewId) } returns null

                Then("EntityNotFoundException이 발생한다") {
                    shouldThrow<EntityNotFoundException> {
                        reviewService.delete(command, memberId)
                    }
                }
            }

            When("다른 사용자의 회고이면") {
                val otherMemberId = MemberId(99L)
                every { reviewRepository.findById(reviewId) } returns savedReview

                Then("AccessDeniedException이 발생한다") {
                    shouldThrow<AccessDeniedException> {
                        reviewService.delete(command, otherMemberId)
                    }
                }
            }
        }
    })
