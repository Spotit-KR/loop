package kr.io.team.loop.learning

/**
 * 학습용 도메인 모델.
 * 실제 프로젝트에서는 DGS Codegen이 스키마에서 자동 생성하지만,
 * 학습 목적으로 직접 작성한다.
 */
data class Show(
    val id: String,
    val title: String,
    val releaseYear: Int? = null,
    // actors, ratings는 별도 DataFetcher로 resolve
)

data class Actor(
    val id: String,
    val name: String,
)

data class Rating(
    val stars: Int,
    val comment: String? = null,
)

data class ShowConnection(
    val content: List<Show>,
    val totalElements: Int,
    val totalPages: Int,
    val currentPage: Int,
)
