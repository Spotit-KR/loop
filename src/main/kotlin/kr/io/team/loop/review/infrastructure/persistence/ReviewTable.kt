package kr.io.team.loop.review.infrastructure.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

private val objectMapper = jacksonObjectMapper()

object ReviewTable : Table("review") {
    val reviewId = long("review_id").autoIncrement()
    val reviewType = text("review_type")
    val memberId = long("member_id").index()
    val steps =
        jsonb<List<StepJson>>(
            "steps",
            serialize = { objectMapper.writeValueAsString(it) },
            deserialize = { objectMapper.readValue(it) },
        )
    val startDate = date("start_date")
    val endDate = date("end_date").nullable()
    val periodKey = text("period_key")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(reviewId)

    init {
        uniqueIndex(memberId, periodKey)
    }
}

data class StepJson(
    val type: String,
    val content: String,
)
