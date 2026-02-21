package kr.io.team.loop.category.application.service

import kr.io.team.loop.category.domain.model.CategoryCommand
import kr.io.team.loop.category.domain.model.CategoryQuery
import kr.io.team.loop.category.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteCategoryService(
    private val categoryRepository: CategoryRepository,
) {
    @Transactional
    fun execute(command: CategoryCommand.Delete) {
        val category =
            categoryRepository.findAll(CategoryQuery(categoryId = command.categoryId)).firstOrNull()
                ?: throw NoSuchElementException("Category not found: ${command.categoryId.value}")
        require(category.memberId == command.memberId) {
            "Not authorized to delete this category"
        }
        categoryRepository.delete(command)
    }
}
