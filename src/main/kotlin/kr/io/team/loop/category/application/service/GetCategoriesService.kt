package kr.io.team.loop.category.application.service

import kr.io.team.loop.category.application.dto.CategoryDto
import kr.io.team.loop.category.domain.model.CategoryQuery
import kr.io.team.loop.category.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetCategoriesService(
    private val categoryRepository: CategoryRepository,
) {
    @Transactional(readOnly = true)
    fun execute(query: CategoryQuery): List<CategoryDto> {
        val categories = categoryRepository.findAll(query)
        return categories.map { CategoryDto.from(it) }
    }
}
