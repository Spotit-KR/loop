package kr.io.team.loop.category.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CategoryColorTest :
    BehaviorSpec({
        given("유효한 색상 코드 #RRGGBB") {
            `when`("CategoryColor 생성") {
                then("성공") {
                    val color = CategoryColor("#FF5733")
                    color.value shouldBe "#FF5733"
                }
            }
        }

        given("소문자 16진수 색상 코드") {
            `when`("CategoryColor 생성") {
                then("성공") {
                    val color = CategoryColor("#ff5733")
                    color.value shouldBe "#ff5733"
                }
            }
        }

        given("# 없는 문자열") {
            `when`("CategoryColor 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        CategoryColor("FF5733")
                    }
                }
            }
        }

        given("잘못된 16진수 문자 포함") {
            `when`("CategoryColor 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        CategoryColor("#GG5733")
                    }
                }
            }
        }

        given("길이가 7이 아닌 문자열") {
            `when`("CategoryColor 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        CategoryColor("#FF573")
                    }
                }
            }
        }
    })
