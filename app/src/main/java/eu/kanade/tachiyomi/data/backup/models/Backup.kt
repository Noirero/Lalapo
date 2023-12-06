package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class Backup(
    @ProtoNumber(3) val backupAnime: List<BackupAnime> = emptyList(),
    @ProtoNumber(4) var backupAnimeCategories: List<BackupCategory> = emptyList(),
    // Bump by 100 to specify this is a 0.x value
    @ProtoNumber(102) var backupBrokenAnimeSources: List<BrokenBackupAnimeSource> = emptyList(),
    @ProtoNumber(103) var backupAnimeSources: List<BackupAnimeSource> = emptyList(),
    @ProtoNumber(104) var backupPreferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(105) var backupExtensionPreferences: List<BackupExtensionPreferences> = emptyList(),
    @ProtoNumber(106) var backupExtensions: List<BackupExtension> = emptyList(),
) {

    companion object {
        val filenameRegex = """${BuildConfig.APPLICATION_ID}_\d+-\d+-\d+_\d+-\d+.tachibk""".toRegex()

        fun getFilename(): String {
            val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            return "${BuildConfig.APPLICATION_ID}_$date.tachibk"
        }
    }
}
