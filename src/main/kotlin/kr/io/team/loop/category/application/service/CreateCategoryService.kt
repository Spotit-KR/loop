package kr.io.team.loop.category.application.service

import kr.io.team.loop.category.application.dto.CategoryDto
import kr.io.team.loop.category.domain.model.CategoryCommand
import kr.io.team.loop.category.domain.model.CategoryQuery
import kr.io.team.loop.category.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateCategoryService(
    private val categoryRepository: CategoryRepository,
) {
    @Transactional
    fun execute(command: CategoryCommand.Create): CategoryDto {
        require(categoryRepository.findAll(CategoryQuery(memberId = command.memberId)).size < 10) {
            "Category limit exceeded: maximum 10 categories per member"
        }
        require(categoryRepository.findAll(CategoryQuery(memberId = command.memberId, name = command.name)).isEmpty()) {
            "Category name already exists: ${command.name.value}"
        }
        val category = categoryRepository.save(command)
        return CategoryDto.from(category)
    }
}
