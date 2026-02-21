package kr.io.team.loop.category.application.service

import kr.io.team.loop.category.application.dto.CategoryDto
import kr.io.team.loop.category.domain.model.CategoryCommand
import kr.io.team.loop.category.domain.model.CategoryQuery
import kr.io.team.loop.category.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateCategoryService(
    private val categoryRepository: CategoryRepository,
) {
    @Transactional
    fun execute(command: CategoryCommand.Update): CategoryDto {
        val category =
            categoryRepository.findAll(CategoryQuery(categoryId = command.categoryId)).firstOrNull()
                ?: throw NoSuchElementException("Category not found: ${command.categoryId.value}")
        require(category.memberId == command.memberId) {
            "Not authorized to update this category"
        }
        if (category.name != command.name) {
            require(
                categoryRepository.findAll(CategoryQuery(memberId = command.memberId, name = command.name)).isEmpty(),
            ) {
                "Category name already exists: ${command.name.value}"
            }
        }
        val updated = categoryRepository.save(command)
        return CategoryDto.from(updated)
    }
}
