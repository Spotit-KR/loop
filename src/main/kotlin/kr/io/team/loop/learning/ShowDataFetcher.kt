package kr.io.team.loop.learning

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import graphql.schema.DataFetchingEnvironment
import java.util.concurrent.CompletableFuture

/**
 * =============================================
 * DGS DataFetcher 학습
 * =============================================
 *
 * @DgsComponent : 이 클래스가 DGS 컴포넌트임을 선언 (Spring의 @Component 확장)
 * @DgsQuery : type Query의 필드를 resolve하는 메서드
 * @DgsMutation : type Mutation의 필드를 resolve하는 메서드
 * @DgsData : 특정 타입의 특정 필드를 resolve하는 메서드 (자식 필드용)
 *
 * 핵심 원리:
 *   GraphQL은 "필드 단위"로 데이터를 가져온다.
 *   클라이언트가 요청한 필드만 resolve되고, 요청하지 않은 필드의 DataFetcher는 호출되지 않는다.
 */
@DgsComponent
class ShowDataFetcher {
    // ===== 인메모리 데이터 (학습용) =====
    private val shows =
        mutableListOf(
            Show("1", "Stranger Things", 2016),
            Show("2", "Ozark", 2017),
            Show("3", "The Crown", 2016),
            Show("4", "Dead to Me", 2019),
            Show("5", "Orange is the New Black", 2013),
        )

    private val actorsByShowId =
        mapOf(
            "1" to listOf(Actor("a1", "Millie Bobby Brown"), Actor("a2", "Finn Wolfhard")),
            "2" to listOf(Actor("a3", "Jason Bateman"), Actor("a4", "Laura Linney")),
            "3" to listOf(Actor("a5", "Olivia Colman"), Actor("a6", "Tobias Menzies")),
            "4" to listOf(Actor("a7", "Christina Applegate"), Actor("a8", "Linda Cardellini")),
            "5" to listOf(Actor("a9", "Taylor Schilling"), Actor("a10", "Laura Prepon")),
        )

    private val ratingsByShowId =
        mapOf(
            "1" to listOf(Rating(5, "Amazing!"), Rating(4, "Great show")),
            "2" to listOf(Rating(5, "Best thriller"), Rating(3)),
            "3" to listOf(Rating(4, "Excellent acting")),
            "4" to listOf(Rating(4), Rating(4, "Funny and sad")),
            "5" to listOf(Rating(3, "Good but long")),
        )

    // =============================================================
    // 1. @DgsQuery - Query 타입의 필드를 resolve
    // =============================================================

    /**
     * schema의 `shows(titleFilter: String): [Show!]!` 에 매핑.
     *
     * 메서드명이 스키마의 필드명과 일치하면 자동 매핑된다.
     * @InputArgument로 GraphQL 인자를 메서드 파라미터로 받는다.
     *
     * 클라이언트 요청 예시:
     * ```graphql
     * query {
     *   shows(titleFilter: "Ozark") {
     *     id
     *     title
     *   }
     * }
     * ```
     */
    @DgsQuery
    fun shows(
        @InputArgument titleFilter: String?,
    ): List<Show> =
        if (titleFilter != null) {
            shows.filter { it.title.contains(titleFilter, ignoreCase = true) }
        } else {
            shows.toList()
        }

    /**
     * schema의 `show(id: ID!): Show` 에 매핑.
     *
     * 반환 타입이 nullable인 것에 주의 — 스키마에서 Show? (non-null 아님)
     */
    @DgsQuery
    fun show(
        @InputArgument id: String,
    ): Show? = shows.find { it.id == id }

    /**
     * 페이지네이션 예시.
     * 기본값은 스키마에서 정의: page: Int = 0, size: Int = 10
     */
    @DgsQuery
    fun showsPaged(
        @InputArgument page: Int?,
        @InputArgument size: Int?,
    ): ShowConnection {
        val p = page ?: 0
        val s = size ?: 10
        val start = p * s
        val paged = shows.drop(start).take(s)
        return ShowConnection(
            content = paged,
            totalElements = shows.size,
            totalPages = (shows.size + s - 1) / s,
            currentPage = p,
        )
    }

    // =============================================================
    // 2. @DgsMutation - Mutation 타입의 필드를 resolve
    // =============================================================

    /**
     * schema의 `addShow(input: AddShowInput!): Show!` 에 매핑.
     *
     * Input 타입은 Map으로 전달되므로, @InputArgument로 직접 필드를 꺼내거나
     * Codegen이 생성한 타입을 사용할 수 있다.
     *
     * 클라이언트 요청 예시:
     * ```graphql
     * mutation {
     *   addShow(input: { title: "Wednesday", releaseYear: 2022 }) {
     *     id
     *     title
     *   }
     * }
     * ```
     */
    @DgsMutation
    fun addShow(
        @InputArgument input: Map<String, Any?>,
    ): Show {
        val newShow =
            Show(
                id = (shows.size + 1).toString(),
                title = input["title"] as String,
                releaseYear = input["releaseYear"] as Int?,
            )
        shows.add(newShow)
        return newShow
    }

    /**
     * 단순 스칼라 인자를 받는 Mutation.
     */
    @DgsMutation
    fun addRating(
        @InputArgument showId: String,
        @InputArgument stars: Int,
    ): Rating = Rating(stars = stars, comment = null)

    // =============================================================
    // 3. @DgsData - 자식 필드(Child Field) resolve
    // =============================================================
    //
    // Show 타입의 actors 필드를 별도 DataFetcher로 분리한 이유:
    //   - shows 쿼리에서 actors를 요청하지 않으면 이 메서드는 호출되지 않음
    //   - 비용이 큰 데이터 로딩을 필요할 때만 수행 (lazy loading)
    //
    // *** 주의: 이 방식은 N+1 문제를 발생시킨다! ***
    //   shows가 5개 → actors DataFetcher가 5번 호출 → DB 쿼리 5번
    //   이걸 해결하는 것이 DataLoader (ShowDataLoader.kt 참고)
    //

    /**
     * Show.actors 필드를 resolve.
     * parentType = "Show"이고 field = "actors"인 필드를 이 메서드가 처리한다.
     *
     * DataFetchingEnvironment로 "부모 객체"에 접근할 수 있다.
     * 여기서 부모는 Show 객체 — shows() 쿼리가 반환한 Show.
     */
    @DgsData(parentType = "Show", field = "actors")
    fun actorsForShow(dfe: DataFetchingEnvironment): List<Actor> {
        // getSource()로 부모 객체(Show)를 가져온다
        val show: Show = dfe.getSource()!!
        // 이 호출이 Show 개수만큼 반복된다 = N+1 문제!
        return actorsByShowId[show.id] ?: emptyList()
    }

    /**
     * Show.ratings 필드를 resolve.
     * 이것도 N+1 문제가 있다. DataLoader 버전은 ShowDataLoader.kt 참고.
     */
    @DgsData(parentType = "Show", field = "ratings")
    fun ratingsForShow(dfe: DataFetchingEnvironment): List<Rating> {
        val show: Show = dfe.getSource()!!
        return ratingsByShowId[show.id] ?: emptyList()
    }
}
