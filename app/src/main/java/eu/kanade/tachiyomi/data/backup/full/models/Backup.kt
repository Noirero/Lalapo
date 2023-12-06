package eu.kanade.tachiyomi.data.backup.full.models

import eu.kanade.tachiyomi.data.backup.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeSource
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BrokenBackupAnimeSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class Backup(
    @ProtoNumber(3) val backupAnime: List<BackupAnime> = emptyList(),
    @ProtoNumber(4) var backupAnimeCategories: List<BackupCategory> = emptyList(),
    // Bump by 100 to specify this is a 0.x value
    @ProtoNumber(102) var backupBrokenAnimeSources: List<BrokenBackupAnimeSource> = emptyList(),
    @ProtoNumber(103) var backupAnimeSources: List<BackupAnimeSource> = emptyList(),
    @ProtoNumber(104) var backupPreferences: List<BackupPreference> = emptyList(),
) {

    companion object {
        fun getBackupFilename(): String {
            val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            return "Animiru_$date.proto.gz"
        }
    }
}
