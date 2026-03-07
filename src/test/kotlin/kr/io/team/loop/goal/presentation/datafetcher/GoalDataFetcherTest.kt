package kr.io.team.loop.goal.presentation.datafetcher

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import kr.io.team.loop.common.config.AuthorizeArgumentResolver
import kr.io.team.loop.common.config.JwtTokenProvider
import kr.io.team.loop.common.domain.GoalId
import kr.io.team.loop.common.domain.MemberId
import kr.io.team.loop.goal.application.service.GoalService
import kr.io.team.loop.goal.domain.model.Goal
import kr.io.team.loop.goal.domain.model.GoalQuery
import kr.io.team.loop.goal.domain.model.GoalTitle
import kr.io.team.loop.task.application.dto.GoalTaskStatsDto
import kr.io.team.loop.task.application.service.TaskService
import kr.io.team.loop.task.presentation.datafetcher.GoalTaskStatsDataFetcher
import kr.io.team.loop.task.presentation.dataloader.GoalTaskStatsDataLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import java.time.Instant

@SpringBootTest(
    classes = [
        GoalDataFetcher::class,
        GoalTaskStatsDataFetcher::class,
        GoalTaskStatsDataLoader::class,
        AuthorizeArgumentResolver::class,
    ],
)
@EnableDgsTest
class GoalDataFetcherTest {
    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockkBean
    lateinit var goalService: GoalService

    @MockkBean
    lateinit var taskService: TaskService

    @MockkBean
    lateinit var jwtTokenProvider: JwtTokenProvider

    private val memberId = 1L
    private val testToken = "test-token"

    private fun authHeaders(): HttpHeaders =
        HttpHeaders().apply {
            setBearerAuth(testToken)
        }

    private fun setupAuth() {
        every { jwtTokenProvider.validateToken(testToken) } returns true
        every { jwtTokenProvider.getMemberIdFromToken(testToken) } returns memberId
    }

    @Test
    fun `myGoals computed fields are resolved by DataLoader, not hardcoded values`() {
        setupAuth()

        val goal =
            Goal(
                id = GoalId(1L),
                title = GoalTitle("Test Goal"),
                memberId = MemberId(memberId),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = null,
            )
        every { goalService.findAll(GoalQuery(memberId = MemberId(memberId))) } returns listOf(goal)
        every { taskService.getStatsByGoalIds(setOf(GoalId(1L))) } returns
            mapOf(
                GoalId(1L) to
                    GoalTaskStatsDto(
                        goalId = GoalId(1L),
                        totalCount = 10,
                        completedCount = 7,
                    ),
            )

        val result =
            dgsQueryExecutor.executeAndGetDocumentContext(
                """
                {
                    myGoals {
                        id
                        title
                        totalTaskCount
                        completedTaskCount
                        achievementRate
                    }
                }
                """.trimIndent(),
                emptyMap<String, Any>(),
                authHeaders(),
            )

        val totalTaskCount: Int = result.read("data.myGoals[0].totalTaskCount")
        val completedTaskCount: Int = result.read("data.myGoals[0].completedTaskCount")
        val achievementRate: Double = result.read("data.myGoals[0].achievementRate")

        assertThat(totalTaskCount).isEqualTo(10)
        assertThat(completedTaskCount).isEqualTo(7)
        assertThat(achievementRate).isEqualTo(70.0)
    }

    @Test
    fun `myGoals returns basic fields correctly`() {
        setupAuth()

        val goal =
            Goal(
                id = GoalId(1L),
                title = GoalTitle("영어 공부"),
                memberId = MemberId(memberId),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = null,
            )
        every { goalService.findAll(GoalQuery(memberId = MemberId(memberId))) } returns listOf(goal)
        every { taskService.getStatsByGoalIds(any()) } returns emptyMap()

        val titles: List<String> =
            dgsQueryExecutor.executeAndExtractJsonPath(
                """
                {
                    myGoals {
                        id
                        title
                    }
                }
                """.trimIndent(),
                "data.myGoals[*].title",
                authHeaders(),
            )

        assertThat(titles).containsExactly("영어 공부")
    }

    @Test
    fun `myGoals computed fields default to 0 when no tasks exist`() {
        setupAuth()

        val goal =
            Goal(
                id = GoalId(1L),
                title = GoalTitle("Empty Goal"),
                memberId = MemberId(memberId),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = null,
            )
        every { goalService.findAll(GoalQuery(memberId = MemberId(memberId))) } returns listOf(goal)
        every { taskService.getStatsByGoalIds(setOf(GoalId(1L))) } returns emptyMap()

        val result =
            dgsQueryExecutor.executeAndGetDocumentContext(
                """
                {
                    myGoals {
                        totalTaskCount
                        completedTaskCount
                        achievementRate
                    }
                }
                """.trimIndent(),
                emptyMap<String, Any>(),
                authHeaders(),
            )

        val totalTaskCount: Int = result.read("data.myGoals[0].totalTaskCount")
        val completedTaskCount: Int = result.read("data.myGoals[0].completedTaskCount")
        val achievementRate: Double = result.read("data.myGoals[0].achievementRate")

        assertThat(totalTaskCount).isEqualTo(0)
        assertThat(completedTaskCount).isEqualTo(0)
        assertThat(achievementRate).isEqualTo(0.0)
    }
}
