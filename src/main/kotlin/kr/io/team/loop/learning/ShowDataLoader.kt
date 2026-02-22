package kr.io.team.loop.learning

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataLoader
import graphql.schema.DataFetchingEnvironment
import org.dataloader.BatchLoader
import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

/**
 * =============================================
 * DataLoader 학습 — N+1 문제 해결
 * =============================================
 *
 * N+1 문제란?
 *   query { shows { actors { name } } }
 *   → shows 쿼리 1번 (Show 5개 반환)
 *   → 각 Show의 actors를 개별 조회 5번
 *   → 총 6번 호출 (1 + N)
 *
 * DataLoader의 해결 방식:
 *   → shows 쿼리 1번 (Show 5개 반환)
 *   → 5개 Show의 actors 요청을 모아서 1번에 배치 조회
 *   → 총 2번 호출 (1 + 1)
 *
 * 동작 원리:
 *   1. 각 Show의 actors DataFetcher가 호출될 때, 바로 DB를 치지 않고
 *      DataLoader에 "이 showId의 actors가 필요해"라고 등록만 한다.
 *   2. 모든 Show에 대해 등록이 끝나면, DataLoader가 축적된 ID들을
 *      한 번에 BatchLoader에 전달한다.
 *   3. BatchLoader가 한 번의 호출로 모든 결과를 가져온다.
 *   4. DataLoader가 결과를 요청자별로 나눠서 돌려준다.
 */

// =============================================================
// 방법 1: BatchLoader — 순서 기반 매핑
// =============================================================
// keys와 results가 같은 순서여야 한다.
// keys = ["1", "2", "3"] → results = [actors_for_1, actors_for_2, actors_for_3]

@DgsDataLoader(name = "actorsForShow")
class ActorsBatchLoader : BatchLoader<String, List<Actor>> {
    // 학습용 인메모리 데이터
    private val actorsByShowId =
        mapOf(
            "1" to listOf(Actor("a1", "Millie Bobby Brown"), Actor("a2", "Finn Wolfhard")),
            "2" to listOf(Actor("a3", "Jason Bateman"), Actor("a4", "Laura Linney")),
            "3" to listOf(Actor("a5", "Olivia Colman"), Actor("a6", "Tobias Menzies")),
            "4" to listOf(Actor("a7", "Christina Applegate"), Actor("a8", "Linda Cardellini")),
            "5" to listOf(Actor("a9", "Taylor Schilling"), Actor("a10", "Laura Prepon")),
        )

    /**
     * 이 메서드는 DGS가 자동으로 호출한다.
     * keys: 축적된 모든 showId 리스트 (예: ["1", "2", "3", "4", "5"])
     *
     * 반환: keys와 같은 순서로 결과 리스트를 반환해야 한다.
     *   keys[0] = "1" → results[0] = actors_for_show_1
     *   keys[1] = "2" → results[1] = actors_for_show_2
     *   ...
     *
     * 실제 프로덕션에서는 여기서 DB 쿼리 1번 실행:
     *   SELECT * FROM actors WHERE show_id IN (keys)
     */
    override fun load(keys: List<String>): CompletionStage<List<List<Actor>>> {
        println("[BatchLoader] actors 배치 로딩 — showIds: $keys")
        val results =
            keys.map { showId ->
                actorsByShowId[showId] ?: emptyList()
            }
        return CompletableFuture.completedFuture(results)
    }
}

// =============================================================
// 방법 2: MappedBatchLoader — Map 기반 매핑
// =============================================================
// 순서를 맞출 필요 없이 Map으로 반환한다.
// 키에 해당하는 결과가 없으면 그냥 Map에서 빠지면 된다.
// "일부 키에 결과가 없을 수 있는 경우"에 더 적합하다.

@DgsDataLoader(name = "ratingsForShow")
class RatingsMappedBatchLoader : MappedBatchLoader<String, List<Rating>> {
    private val ratingsByShowId =
        mapOf(
            "1" to listOf(Rating(5, "Amazing!"), Rating(4, "Great show")),
            "2" to listOf(Rating(5, "Best thriller"), Rating(3)),
            "3" to listOf(Rating(4, "Excellent acting")),
            "4" to listOf(Rating(4), Rating(4, "Funny and sad")),
            "5" to listOf(Rating(3, "Good but long")),
        )

    /**
     * MappedBatchLoader는 Map<Key, Value>를 반환한다.
     * BatchLoader와 달리 순서를 맞출 필요 없다.
     */
    override fun load(keys: Set<String>): CompletionStage<Map<String, List<Rating>>> {
        println("[MappedBatchLoader] ratings 배치 로딩 — showIds: $keys")
        val results =
            keys.associateWith { showId ->
                ratingsByShowId[showId] ?: emptyList()
            }
        return CompletableFuture.completedFuture(results)
    }
}

// =============================================================
// DataLoader를 사용하는 DataFetcher
// =============================================================
// ShowDataFetcher의 N+1 버전 대신 이것을 사용하면 배치 처리된다.
// 동시에 활성화하면 충돌하므로, 학습 시 하나만 사용할 것.
//
// 아래 클래스는 주석 처리 — ShowDataFetcher의 @DgsData와 동일 필드를 resolve하므로.
// 실제로 사용하려면 ShowDataFetcher의 actorsForShow, ratingsForShow를 제거하고
// 아래 주석을 해제한다.

/*
@DgsComponent
class ShowDataLoaderFetcher {

    /**
 * DataLoader 버전의 actors 필드 resolver.
 *
 * 차이점:
 *   - 반환 타입이 CompletableFuture<List<Actor>>
 *   - 직접 DB를 치지 않고, DataLoader에 "load 요청"만 등록
 *   - DGS가 모든 Show의 요청을 모아서 BatchLoader.load()를 한 번 호출
 */
    @DgsData(parentType = "Show", field = "actors")
    fun actorsForShow(dfe: DataFetchingEnvironment): CompletableFuture<List<Actor>> {
        val show: Show = dfe.getSource()

        // DataLoader 가져오기 (이름으로 조회)
        val dataLoader = dfe.getDataLoader<String, List<Actor>>("actorsForShow")

        // load() 호출 = "이 키의 데이터가 필요해"를 등록
        // 실제 로딩은 나중에 배치로 처리됨
        return dataLoader.load(show.id)
    }

    @DgsData(parentType = "Show", field = "ratings")
    fun ratingsForShow(dfe: DataFetchingEnvironment): CompletableFuture<List<Rating>> {
        val show: Show = dfe.getSource()
        val dataLoader = dfe.getDataLoader<String, List<Rating>>("ratingsForShow")
        return dataLoader.load(show.id)
    }
}
*/
