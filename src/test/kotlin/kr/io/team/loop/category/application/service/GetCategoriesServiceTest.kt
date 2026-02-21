package kr.io.team.loop.category.application.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kr.io.team.loop.category.domain.model.Category
import kr.io.team.loop.category.domain.model.CategoryColor
import kr.io.team.loop.category.domain.model.CategoryName
import kr.io.team.loop.category.domain.model.CategoryQuery
import kr.io.team.loop.category.domain.model.SortOrder
import kr.io.team.loop.category.domain.repository.CategoryRepository
import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import java.time.LocalDateTime

class GetCategoriesServiceTest :
    BehaviorSpec({
        val categoryRepository = mockk<CategoryRepository>()
        val service = GetCategoriesService(categoryRepository)

        val memberId = MemberId(1L)
        val now = LocalDateTime.now()
        val categories =
            listOf(
                Category(
                    id = CategoryId(1L),
                    memberId = memberId,
                    name = CategoryName("운동"),
                    color = CategoryColor("#FF5733"),
                    sortOrder = SortOrder(0),
                    createdAt = now,
                    updatedAt = now,
                ),
                Category(
                    id = CategoryId(2L),
                    memberId = memberId,
                    name = CategoryName("공부"),
                    color = CategoryColor("#33BFFF"),
                    sortOrder = SortOrder(1),
                    createdAt = now,
                    updatedAt = now,
                ),
            )

        given("회원 ID") {
            val query = CategoryQuery(memberId = memberId)

            `when`("execute 호출") {
                every { categoryRepository.findAll(query) } returns categories

                then("CategoryDto 목록 반환") {
                    val result = service.execute(query)
                    result.size shouldBe 2
                    result[0].name.value shouldBe "운동"
                    result[1].name.value shouldBe "공부"
                }
            }
        }

        given("카테고리가 없는 회원") {
            val emptyMemberId = MemberId(2L)
            val query = CategoryQuery(memberId = emptyMemberId)

            `when`("execute 호출") {
                every { categoryRepository.findAll(query) } returns emptyList()

                then("빈 목록 반환") {
                    val result = service.execute(query)
                    result.size shouldBe 0
                }
            }
        }
    })
