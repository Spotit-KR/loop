package kr.io.team.loop.category.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CategoryNameTest :
    BehaviorSpec({
        given("유효한 카테고리명") {
            `when`("CategoryName 생성") {
                then("성공") {
                    val name = CategoryName("운동")
                    name.value shouldBe "운동"
                }
            }
        }

        given("빈 문자열") {
            `when`("CategoryName 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        CategoryName("")
                    }
                }
            }
        }

        given("공백만 있는 문자열") {
            `when`("CategoryName 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        CategoryName("   ")
                    }
                }
            }
        }

        given("50자 이름") {
            `when`("CategoryName 생성") {
                then("성공") {
                    val name = CategoryName("a".repeat(50))
                    name.value.length shouldBe 50
                }
            }
        }

        given("51자 이름") {
            `when`("CategoryName 생성") {
                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        CategoryName("a".repeat(51))
                    }
                }
            }
        }
    })
