package wayzer.user

import java.io.Serializable
import java.time.Duration
import java.time.Instant

data class PlayerBan(
    val recordId: Int,
    val ids: Set<String>,
    val reason: String,
    val operator: String?,
    val createTime: Instant,
    val endTime: Instant
) : Serializable

interface PlayerBanStore {
    fun findNotEnd(id: String): PlayerBan?
    fun create(
        ids: Set<String>,
        duration: Duration,
        reason: String,
        operator: String?
    ): PlayerBan

    fun delete(record: Int): PlayerBan?
}