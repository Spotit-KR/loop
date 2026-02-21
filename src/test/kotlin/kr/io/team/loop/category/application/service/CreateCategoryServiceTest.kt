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

class CreateCategoryServiceTest :
    BehaviorSpec({
        val categoryRepository = mockk<CategoryRepository>()
        val service = CreateCategoryService(categoryRepository)

        val command =
            CategoryCommand.Create(
                memberId = MemberId(1L),
                name = CategoryName("운동"),
                color = CategoryColor("#FF5733"),
                sortOrder = SortOrder(0),
            )
        val now = LocalDateTime.now()
        val category =
            Category(
                id = CategoryId(1L),
                memberId = command.memberId,
                name = command.name,
                color = command.color,
                sortOrder = command.sortOrder,
                createdAt = now,
                updatedAt = now,
            )

        given("유효한 커맨드 (카테고리 수 < 10, 이름 중복 없음)") {
            `when`("execute 호출") {
                every {
                    categoryRepository.findAll(CategoryQuery(memberId = command.memberId))
                } returns emptyList()
                every {
                    categoryRepository.findAll(CategoryQuery(memberId = command.memberId, name = command.name))
                } returns emptyList()
                every { categoryRepository.save(command) } returns category

                then("CategoryDto 반환") {
                    val result = service.execute(command)
                    result.id.value shouldBe 1L
                    result.name.value shouldBe "운동"
                    result.color.value shouldBe "#FF5733"
                }
            }
        }

        given("카테고리 수가 이미 10개") {
            `when`("execute 호출") {
                every {
                    categoryRepository.findAll(CategoryQuery(memberId = command.memberId))
                } returns List(10) { category }

                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        service.execute(command)
                    }
                }
            }
        }

        given("동일 회원 내 카테고리명 중복") {
            `when`("execute 호출") {
                every {
                    categoryRepository.findAll(CategoryQuery(memberId = command.memberId))
                } returns List(3) { category }
                every {
                    categoryRepository.findAll(CategoryQuery(memberId = command.memberId, name = command.name))
                } returns listOf(category)

                then("IllegalArgumentException 발생") {
                    shouldThrow<IllegalArgumentException> {
                        service.execute(command)
                    }
                }
            }
        }
    })
