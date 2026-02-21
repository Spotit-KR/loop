package kr.io.team.loop.category.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
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

class UpdateCategoryServiceTest :
    BehaviorSpec({
        val categoryRepository = mockk<CategoryRepository>()
        val service = UpdateCategoryService(categoryRepository)

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

        given("유효한 수정 커맨드 (이름 변경, 중복 없음)") {
            val command =
                CategoryCommand.Update(
                    categoryId = categoryId,
                    memberId = memberId,
                    name = CategoryName("공부"),
                    color = CategoryColor("#33BFFF"),
                    sortOrder = SortOrder(1),
                )
            val updatedCategory =
                existingCategory.copy(
                    name = command.name,
                    color = command.color,
                    sortOrder = command.sortOrder,
                    updatedAt = now,
                )

            `when`("execute 호출") {
                every { categoryRepository.findAll(CategoryQuery(categoryId = categoryId)) } returns
                    listOf(existingCategory)
                every {
                    categoryRepository.findAll(CategoryQuery(memberId = memberId, name = command.name))
                } returns emptyList()
                every { categoryRepository.save(command) } returns updatedCategory

                then("CategoryDto 반환") {
                    val result = service.execute(command)
                    result.name.value shouldBe "공부"
                    result.color.value shouldBe "#33BFFF"
                }
            }
        }

        given("존재하지 않는 카테고리") {
            val command =
                CategoryCommand.Update(
                    categoryId = CategoryId(999L),
                    memberId = memberId,
                    name = CategoryName("공부"),
                    color = CategoryColor("#33BFFF"),
                    sortOrder = SortOrder(0),
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

        given("다른 회원의 카테고리 수정 시도") {
            val command =
                CategoryCommand.Update(
                    categoryId = categoryId,
                    memberId = MemberId(2L),
                    name = CategoryName("공부"),
                    color = CategoryColor("#33BFFF"),
                    sortOrder = SortOrder(0),
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

        given("동일 회원 내 카테고리명 중복") {
            val command =
                CategoryCommand.Update(
                    categoryId = categoryId,
                    memberId = memberId,
                    name = CategoryName("중복이름"),
                    color = CategoryColor("#33BFFF"),
                    sortOrder = SortOrder(0),
                )

            `when`("execute 호출") {
                every { categoryRepository.findAll(CategoryQuery(categoryId = categoryId)) } returns
                    listOf(existingCategory)
                every {
                    categoryRepository.findAll(CategoryQuery(memberId = memberId, name = command.name))
                } returns listOf(existingCategory)

                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        service.execute(command)
                    }
                }
            }
        }
    })
