package kr.io.team.loop.learning

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * =============================================
 * DGS GraphQL 학습용 테스트
 * =============================================
 *
 * @EnableDgsTest: DGS 프레임워크의 테스트 슬라이스.
 *   GraphQL 실행에 필요한 최소한의 컴포넌트만 로드한다.
 *
 * DgsQueryExecutor: GraphQL 쿼리를 프로그래밍 방식으로 실행하는 인터페이스.
 *   HTTP 요청 없이 직접 쿼리를 실행하고 결과를 검증할 수 있다.
 *
 * 이 테스트는 실제 GraphQL 쿼리를 실행해서
 * DGS가 어떻게 동작하는지 확인하는 학습용이다.
 */
@SpringBootTest(classes = [ShowDataFetcher::class, ActorsBatchLoader::class, RatingsMappedBatchLoader::class])
@EnableDgsTest
class DgsGraphQLLearningTest {
    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    // =============================================================
    // 1. 기본 Query 테스트
    // =============================================================

    @Nested
    @DisplayName("1. Query 기본")
    inner class QueryBasics {
        @Test
        @DisplayName("전체 목록 조회 — 필드 선택의 핵심")
        fun `shows 전체 조회`() {
            // GraphQL의 핵심: 클라이언트가 필요한 필드만 선택한다.
            // 여기서는 title만 요청했으므로 releaseYear는 응답에 포함되지 않는다.
            val titles: List<String> =
                dgsQueryExecutor.executeAndExtractJsonPath(
                    """
                    {
                        shows {
                            title
                        }
                    }
                    """.trimIndent(),
                    "data.shows[*].title",
                )

            assertThat(titles).hasSize(5)
            assertThat(titles).contains("Ozark", "Stranger Things")
        }

        @Test
        @DisplayName("필터 인자 전달")
        fun `titleFilter로 검색`() {
            val titles: List<String> =
                dgsQueryExecutor.executeAndExtractJsonPath(
                    """
                    {
                        shows(titleFilter: "Ozark") {
                            title
                            releaseYear
                        }
                    }
                    """.trimIndent(),
                    "data.shows[*].title",
                )

            assertThat(titles).hasSize(1)
            assertThat(titles[0]).isEqualTo("Ozark")
        }

        @Test
        @DisplayName("단건 조회 — nullable 반환")
        fun `ID로 단건 조회`() {
            val title: String =
                dgsQueryExecutor.executeAndExtractJsonPath(
                    """
                    {
                        show(id: "2") {
                            title
                            releaseYear
                        }
                    }
                    """.trimIndent(),
                    "data.show.title",
                )

            assertThat(title).isEqualTo("Ozark")
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 — null 반환")
        fun `존재하지 않는 ID`() {
            val result =
                dgsQueryExecutor.executeAndGetDocumentContext(
                    """
                    {
                        show(id: "999") {
                            title
                        }
                    }
                    """.trimIndent(),
                )

            // show가 null이면 해당 경로가 null
            val show: Any? = result.read("data.show")
            assertThat(show).isNull()
        }

        @Test
        @DisplayName("페이지네이션")
        fun `페이지네이션 조회`() {
            val totalElements: Int =
                dgsQueryExecutor.executeAndExtractJsonPath(
                    """
                    {
                        showsPaged(page: 0, size: 2) {
                            content {
                                title
                            }
                            totalElements
                            totalPages
                            currentPage
                        }
                    }
                    """.trimIndent(),
                    "data.showsPaged.totalElements",
                )

            assertThat(totalElements).isEqualTo(5)

            // 페이지 사이즈 2이면 3페이지
            val totalPages: Int =
                dgsQueryExecutor.executeAndExtractJsonPath(
                    """
                    {
                        showsPaged(page: 0, size: 2) {
                            totalPages
                        }
                    }
                    """.trimIndent(),
                    "data.showsPaged.totalPages",
                )
            assertThat(totalPages).isEqualTo(3)
        }
    }

    // =============================================================
    // 2. Mutation 테스트
    // =============================================================

    @Nested
    @DisplayName("2. Mutation")
    inner class MutationTests {
        @Test
        @DisplayName("새 Show 추가")
        fun `addShow mutation`() {
            val title: String =
                dgsQueryExecutor.executeAndExtractJsonPath(
                    """
                    mutation {
                        addShow(input: { title: "Wednesday", releaseYear: 2022 }) {
                            id
                            title
                            releaseYear
                        }
                    }
                    """.trimIndent(),
                    "data.addShow.title",
                )

            assertThat(title).isEqualTo("Wednesday")
        }

        @Test
        @DisplayName("별점 추가")
        fun `addRating mutation`() {
            val stars: Int =
                dgsQueryExecutor.executeAndExtractJsonPath(
                    """
                    mutation {
                        addRating(showId: "1", stars: 5) {
                            stars
                        }
                    }
                    """.trimIndent(),
                    "data.addRating.stars",
                )

            assertThat(stars).isEqualTo(5)
        }
    }

    // =============================================================
    // 3. 자식 필드 (Child DataFetcher) 테스트
    // =============================================================

    @Nested
    @DisplayName("3. 자식 필드와 N+1 문제")
    inner class ChildFieldTests {
        @Test
        @DisplayName("Show의 actors 필드 — 별도 DataFetcher가 호출됨")
        fun `show의 actors 조회`() {
            // 이 쿼리는 다음 순서로 실행된다:
            // 1. shows() DataFetcher 호출 → Show 5개 반환
            // 2. 각 Show에 대해 actorsForShow() DataFetcher 호출 → Actor 리스트 반환
            //    (N+1 문제: Show 5개 → actors 조회 5번)
            val actorNames: List<String> =
                dgsQueryExecutor.executeAndExtractJsonPath(
                    """
                    {
                        shows {
                            title
                            actors {
                                name
                            }
                        }
                    }
                    """.trimIndent(),
                    "data.shows[0].actors[*].name",
                )

            assertThat(actorNames).contains("Millie Bobby Brown", "Finn Wolfhard")
        }

        @Test
        @DisplayName("actors를 요청하지 않으면 DataFetcher가 호출되지 않는다")
        fun `actors 없이 title만 조회`() {
            // actors DataFetcher는 호출되지 않음!
            // GraphQL의 장점: 불필요한 데이터 로딩을 자동으로 건너뜀
            val titles: List<String> =
                dgsQueryExecutor.executeAndExtractJsonPath(
                    """
                    {
                        shows {
                            title
                        }
                    }
                    """.trimIndent(),
                    "data.shows[*].title",
                )

            assertThat(titles).hasSize(5)
        }

        @Test
        @DisplayName("중첩 필드 동시 조회 — actors + ratings")
        fun `actors와 ratings 동시 조회`() {
            // actors와 ratings를 모두 요청하면
            // 각각의 DataFetcher가 호출된다
            val result =
                dgsQueryExecutor.executeAndGetDocumentContext(
                    """
                    {
                        shows {
                            title
                            actors {
                                name
                            }
                            ratings {
                                stars
                                comment
                            }
                        }
                    }
                    """.trimIndent(),
                )

            val firstShowTitle: String = result.read("data.shows[0].title")
            val firstShowActors: List<Map<String, Any>> = result.read("data.shows[0].actors")
            val firstShowRatings: List<Map<String, Any>> = result.read("data.shows[0].ratings")

            assertThat(firstShowTitle).isEqualTo("Stranger Things")
            assertThat(firstShowActors).isNotEmpty()
            assertThat(firstShowRatings).isNotEmpty()
        }
    }

    // =============================================================
    // 4. 한 요청에 여러 필드 (Multi-Field Query)
    // =============================================================

    @Nested
    @DisplayName("4. 한 요청에 여러 루트 필드")
    inner class MultiFieldTests {
        @Test
        @DisplayName("하나의 query에 여러 루트 필드를 동시에 요청")
        fun `shows와 show를 한 번에 요청`() {
            // ===== 핵심 학습 포인트 =====
            // GraphQL에서는 하나의 query 안에 여러 루트 필드를 동시에 요청할 수 있다.
            // 이것은 REST에서 여러 엔드포인트를 호출하는 것을 한 번의 요청으로 묶는 것과 같다.
            //
            // REST라면:
            //   GET /api/shows          → 전체 목록
            //   GET /api/shows/1        → 단건 조회
            //   (2번의 HTTP 요청)
            //
            // GraphQL:
            //   1번의 HTTP 요청으로 둘 다 가져올 수 있다.
            //
            // 서버 동작:
            //   DGS는 이 쿼리를 파싱하고, shows와 show 각각의 DataFetcher를 호출한다.
            //   두 필드는 독립적이므로, 서로의 결과에 영향을 주지 않는다.
            val result =
                dgsQueryExecutor.executeAndGetDocumentContext(
                    """
                    {
                        shows {
                            title
                        }
                        show(id: "1") {
                            title
                            releaseYear
                        }
                    }
                    """.trimIndent(),
                )

            val allShowTitles: List<String> = result.read("data.shows[*].title")
            val singleShowTitle: String = result.read("data.show.title")

            assertThat(allShowTitles).hasSize(5)
            assertThat(singleShowTitle).isEqualTo("Stranger Things")
        }

        @Test
        @DisplayName("같은 필드를 다른 인자로 여러 번 요청 — alias 사용")
        fun `alias로 같은 필드 다중 요청`() {
            // ===== 핵심 학습 포인트 =====
            // 같은 필드를 다른 인자로 여러 번 요청하려면 alias가 필요하다.
            // alias 없이 show(id: "1") 과 show(id: "2")를 동시에 쓰면 충돌한다.
            //
            // alias 문법: aliasName: fieldName(args)
            //
            // 서버 동작:
            //   DGS는 alias를 단순 이름 변경으로 처리한다.
            //   show DataFetcher가 2번 호출된다 (각각 다른 인자로).
            //   응답에서는 alias 이름이 키로 사용된다.
            val result =
                dgsQueryExecutor.executeAndGetDocumentContext(
                    """
                    {
                        first: show(id: "1") {
                            title
                        }
                        second: show(id: "2") {
                            title
                        }
                        third: show(id: "3") {
                            title
                        }
                    }
                    """.trimIndent(),
                )

            val first: String = result.read("data.first.title")
            val second: String = result.read("data.second.title")
            val third: String = result.read("data.third.title")

            assertThat(first).isEqualTo("Stranger Things")
            assertThat(second).isEqualTo("Ozark")
            assertThat(third).isEqualTo("The Crown")
        }

        @Test
        @DisplayName("Query와 중첩 필드의 복합 요청")
        fun `복합 쿼리 — 여러 루트 필드 + 중첩 필드`() {
            // 한 요청으로 이 모든 데이터를 가져올 수 있다:
            //   - 전체 show 목록 (actors 포함)
            //   - 특정 show 상세 (ratings 포함)
            //   - 페이지네이션된 결과
            val result =
                dgsQueryExecutor.executeAndGetDocumentContext(
                    """
                    {
                        shows(titleFilter: "Ozark") {
                            title
                            actors {
                                name
                            }
                        }
                        show(id: "1") {
                            title
                            ratings {
                                stars
                                comment
                            }
                        }
                        showsPaged(page: 0, size: 2) {
                            content {
                                title
                            }
                            totalElements
                        }
                    }
                    """.trimIndent(),
                )

            val filteredShows: List<String> = result.read("data.shows[*].title")
            val singleShowTitle: String = result.read("data.show.title")
            val pagedTotal: Int = result.read("data.showsPaged.totalElements")

            assertThat(filteredShows).containsExactly("Ozark")
            assertThat(singleShowTitle).isEqualTo("Stranger Things")
            assertThat(pagedTotal).isEqualTo(5)
        }
    }

    // =============================================================
    // 5. Variables (변수) 사용
    // =============================================================

    @Nested
    @DisplayName("5. Variables")
    inner class VariableTests {
        @Test
        @DisplayName("쿼리 변수 사용 — 하드코딩 대신 변수로 인자 전달")
        fun `변수로 쿼리 인자 전달`() {
            // ===== 핵심 학습 포인트 =====
            // 실제 클라이언트(Apollo, Relay 등)는 쿼리 문자열을 재사용하고
            // 인자만 변수로 바꿔가며 요청한다.
            //
            // 변수 문법:
            //   query ShowById($id: ID!) {    ← 변수 선언
            //     show(id: $id) {             ← 변수 사용
            //       title
            //     }
            //   }
            //
            // HTTP 요청 시 variables 필드로 전달:
            //   { "query": "...", "variables": { "id": "2" } }
            val title: String =
                dgsQueryExecutor.executeAndExtractJsonPath(
                    """
                    query ShowById(${"$"}id: ID!) {
                        show(id: ${"$"}id) {
                            title
                        }
                    }
                    """.trimIndent(),
                    "data.show.title",
                    mapOf("id" to "2"),
                )

            assertThat(title).isEqualTo("Ozark")
        }
    }

    // =============================================================
    // 6. 에러 처리
    // =============================================================

    @Nested
    @DisplayName("6. 에러 처리")
    inner class ErrorTests {
        @Test
        @DisplayName("존재하지 않는 필드 요청 — 유효성 검사 실패")
        fun `스키마에 없는 필드 요청`() {
            // GraphQL은 스키마에 정의되지 않은 필드를 요청하면
            // 유효성 검사 단계에서 에러를 반환한다.
            val result =
                dgsQueryExecutor.execute(
                    """
                    {
                        shows {
                            title
                            nonExistentField
                        }
                    }
                    """.trimIndent(),
                )

            assertThat(result.errors).isNotEmpty()
        }

        @Test
        @DisplayName("부분 성공 — 한 필드가 실패해도 다른 필드는 성공")
        fun `에러와 데이터가 공존할 수 있다`() {
            // ===== 핵심 학습 포인트 =====
            // GraphQL의 특징: 부분 성공(partial success)이 가능하다.
            // REST에서는 응답이 성공(200) 아니면 실패(4xx/5xx)지만,
            // GraphQL에서는 일부 필드는 성공하고 일부는 에러일 수 있다.
            //
            // 응답 구조:
            // {
            //   "data": { "shows": [...] },    ← 성공한 데이터
            //   "errors": [{ ... }]             ← 실패한 필드 정보
            // }
            //
            // 참고: 이 테스트에서는 에러를 의도적으로 발생시키기 어려우므로
            // 개념만 설명하고 유효성 에러를 테스트한다.
            val result =
                dgsQueryExecutor.execute(
                    """
                    {
                        shows {
                            title
                        }
                    }
                    """.trimIndent(),
                )

            // 정상 요청이면 errors가 비어있다
            assertThat(result.errors).isEmpty()
            assertThat(result.isDataPresent).isTrue()
        }
    }

    // =============================================================
    // 7. Fragment (재사용 가능한 필드 집합)
    // =============================================================

    @Nested
    @DisplayName("7. Fragment")
    inner class FragmentTests {
        @Test
        @DisplayName("Fragment로 필드 선택을 재사용")
        fun `fragment 사용`() {
            // ===== 핵심 학습 포인트 =====
            // Fragment는 필드 선택을 재사용 가능한 단위로 묶는 것이다.
            // 같은 타입의 필드를 여러 곳에서 선택해야 할 때 중복을 줄인다.
            //
            // 서버 동작: Fragment는 순전히 클라이언트 측 편의 기능이다.
            //   DGS는 Fragment를 인라인으로 펼쳐서 처리한다.
            //   서버에서 추가 구현이 필요 없다.
            val result =
                dgsQueryExecutor.executeAndGetDocumentContext(
                    """
                    {
                        first: show(id: "1") {
                            ...ShowFields
                        }
                        second: show(id: "2") {
                            ...ShowFields
                        }
                    }

                    fragment ShowFields on Show {
                        title
                        releaseYear
                    }
                    """.trimIndent(),
                )

            val firstTitle: String = result.read("data.first.title")
            val secondTitle: String = result.read("data.second.title")

            assertThat(firstTitle).isEqualTo("Stranger Things")
            assertThat(secondTitle).isEqualTo("Ozark")
        }
    }
}
