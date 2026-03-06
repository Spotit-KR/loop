package kr.io.team.loop.review.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.io.team.loop.common.domain.exception.InvalidInputException

class ReviewIdTest :
    BehaviorSpec({

        Given("ReviewId 생성 시") {
            When("양수 값이면") {
                val reviewId = ReviewId(1L)

                Then("정상 생성된다") {
                    reviewId.value shouldBe 1L
                }
            }

            When("0이면") {
                Then("InvalidInputException이 발생한다") {
                    shouldThrow<InvalidInputException> {
                        ReviewId(0L)
                    }
                }
            }

            When("음수이면") {
                Then("InvalidInputException이 발생한다") {
                    shouldThrow<InvalidInputException> {
                        ReviewId(-1L)
                    }
                }
            }
        }
    })
