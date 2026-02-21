package kr.io.team.loop.category.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SortOrderTest :
    BehaviorSpec({
        given("0") {
            `when`("SortOrder 생성") {
                then("성공") {
                    val sortOrder = SortOrder(0)
                    sortOrder.value shouldBe 0
                }
            }
        }

        given("양수") {
            `when`("SortOrder 생성") {
                then("성공") {
                    val sortOrder = SortOrder(5)
                    sortOrder.value shouldBe 5
                }
            }
        }

        given("음수") {
            `when`("SortOrder 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        SortOrder(-1)
                    }
                }
            }
        }
    })
