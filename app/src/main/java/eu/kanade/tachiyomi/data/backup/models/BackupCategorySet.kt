package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupCategorySet(
    @ProtoNumber(1) val syncKey: String,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val order: Long,
    // Category IDs are backup-local and must be remapped after category restore.
    @ProtoNumber(4) val categoryIds: List<Long> = emptyList(),
)
