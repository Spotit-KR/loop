package kr.io.team.loop.health.presentation.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery

@DgsComponent
class HealthDataFetcher {
    @DgsQuery(field = "_health")
    fun health(): Boolean = true
}
