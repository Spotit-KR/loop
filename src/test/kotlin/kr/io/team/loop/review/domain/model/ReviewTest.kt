package kr.io.team.loop.review.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kr.io.team.loop.common.domain.MemberId
import java.time.Instant

class ReviewTest :
    BehaviorSpec({

        val memberId = MemberId(1L)
        val otherMemberId = MemberId(2L)

        val review =
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
                startDate = LocalDate(2026, 2, 20),
                endDate = null,
                periodKey = PeriodKey.daily(LocalDate(2026, 2, 20)),
                createdAt = Instant.now(),
                updatedAt = null,
            )

        Given("Review 소유권 확인 시") {
            When("본인 회고이면") {
                Then("true를 반환한다") {
                    review.isOwnedBy(memberId) shouldBe true
                }
            }

            When("다른 사용자의 회고이면") {
                Then("false를 반환한다") {
                    review.isOwnedBy(otherMemberId) shouldBe false
                }
            }
        }

        Given("Review에 특정 StepType 포함 여부 확인 시") {
            When("KEEP이 포함되어 있으면") {
                Then("true를 반환한다") {
                    review.containsStepType(StepType.KEEP) shouldBe true
                }
            }

            When("포함되지 않은 StepType이면") {
                val reviewWithoutTry =
                    review.copy(
                        steps =
                            listOf(
                                ReviewStep(type = StepType.KEEP, content = "좋은 점"),
                            ),
                    )

                Then("false를 반환한다") {
                    reviewWithoutTry.containsStepType(StepType.TRY) shouldBe false
                }
            }
        }
    })
