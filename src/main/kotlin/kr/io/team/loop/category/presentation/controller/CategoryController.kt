package kr.io.team.loop.category.presentation.controller

import kr.io.team.loop.category.application.service.CreateCategoryService
import kr.io.team.loop.category.application.service.DeleteCategoryService
import kr.io.team.loop.category.application.service.GetCategoriesService
import kr.io.team.loop.category.application.service.UpdateCategoryService
import kr.io.team.loop.category.domain.model.CategoryColor
import kr.io.team.loop.category.domain.model.CategoryCommand
import kr.io.team.loop.category.domain.model.CategoryName
import kr.io.team.loop.category.domain.model.CategoryQuery
import kr.io.team.loop.category.domain.model.SortOrder
import kr.io.team.loop.category.presentation.request.CreateCategoryRequest
import kr.io.team.loop.category.presentation.request.UpdateCategoryRequest
import kr.io.team.loop.category.presentation.response.CategoryResponse
import kr.io.team.loop.common.config.CurrentMemberId
import kr.io.team.loop.common.domain.CategoryId
import kr.io.team.loop.common.domain.MemberId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/categories")
class CategoryController(
    private val createCategoryService: CreateCategoryService,
    private val getCategoriesService: GetCategoriesService,
    private val updateCategoryService: UpdateCategoryService,
    private val deleteCategoryService: DeleteCategoryService,
) {
    @PostMapping
    fun createCategory(
        @RequestBody request: CreateCategoryRequest,
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<CategoryResponse> {
        val command =
            CategoryCommand.Create(
                memberId = MemberId(memberId),
                name = CategoryName(request.name),
                color = CategoryColor(request.color),
                sortOrder = SortOrder(request.sortOrder),
            )
        val dto = createCategoryService.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(dto))
    }

    @GetMapping
    fun getCategories(
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<List<CategoryResponse>> {
        val dtos = getCategoriesService.execute(CategoryQuery(memberId = MemberId(memberId)))
        return ResponseEntity.ok(dtos.map { CategoryResponse.from(it) })
    }

    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @RequestBody request: UpdateCategoryRequest,
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<CategoryResponse> {
        val command =
            CategoryCommand.Update(
                categoryId = CategoryId(id),
                memberId = MemberId(memberId),
                name = CategoryName(request.name),
                color = CategoryColor(request.color),
                sortOrder = SortOrder(request.sortOrder),
            )
        val dto = updateCategoryService.execute(command)
        return ResponseEntity.ok(CategoryResponse.from(dto))
    }

    @DeleteMapping("/{id}")
    fun deleteCategory(
        @PathVariable id: Long,
        @CurrentMemberId memberId: Long,
    ): ResponseEntity<Void> {
        val command =
            CategoryCommand.Delete(
                categoryId = CategoryId(id),
                memberId = MemberId(memberId),
            )
        deleteCategoryService.execute(command)
        return ResponseEntity.noContent().build()
    }
}
