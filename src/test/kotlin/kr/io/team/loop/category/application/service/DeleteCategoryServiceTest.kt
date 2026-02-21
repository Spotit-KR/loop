package kr.io.team.loop.category.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.io.team.loop.category.domain.model.Category
import kr.io.team.loop.category.domain.model.CategoryColor
import kr.io.team.loop.category.domain.model.CategoryCommand
import kr.io.team.loop.category.domain.model.CategoryName
import kr.io.team.loop.category.domain.model.CategoryQuery
import kr.io.team.loop.category.domain.model.SortOrder
import kr.io.team.loop.category.domain.repository.CategoryRepository
import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import java.time.LocalDateTime

class DeleteCategoryServiceTest :
    BehaviorSpec({
        val categoryRepository = mockk<CategoryRepository>()
        val service = DeleteCategoryService(categoryRepository)

        val memberId = MemberId(1L)
        val categoryId = CategoryId(1L)
        val now = LocalDateTime.now()
        val existingCategory =
            Category(
                id = categoryId,
                memberId = memberId,
                name = CategoryName("운동"),
                color = CategoryColor("#FF5733"),
                sortOrder = SortOrder(0),
                createdAt = now,
                updatedAt = now,
            )

        given("유효한 삭제 커맨드") {
            val command =
                CategoryCommand.Delete(
                    categoryId = categoryId,
                    memberId = memberId,
                )

            `when`("execute 호출") {
                every { categoryRepository.findAll(CategoryQuery(categoryId = categoryId)) } returns
                    listOf(existingCategory)
                every { categoryRepository.delete(command) } returns existingCategory

                then("삭제 성공") {
                    service.execute(command)
                    verify { categoryRepository.delete(command) }
                }
            }
        }

        given("존재하지 않는 카테고리") {
            val command =
                CategoryCommand.Delete(
                    categoryId = CategoryId(999L),
                    memberId = memberId,
                )

            `when`("execute 호출") {
                every { categoryRepository.findAll(CategoryQuery(categoryId = CategoryId(999L))) } returns emptyList()

                then("NoSuchElementException 발생") {
                    shouldThrow<NoSuchElementException> {
                        service.execute(command)
                    }
                }
            }
        }

        given("다른 회원의 카테고리 삭제 시도") {
            val command =
                CategoryCommand.Delete(
                    categoryId = categoryId,
                    memberId = MemberId(2L),
                )

            `when`("execute 호출") {
                every { categoryRepository.findAll(CategoryQuery(categoryId = categoryId)) } returns
                    listOf(existingCategory)

                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        service.execute(command)
                    }
                }
            }
        }
    })
