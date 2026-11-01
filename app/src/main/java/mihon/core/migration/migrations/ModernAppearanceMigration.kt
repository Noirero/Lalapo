package mihon.core.migration.migrations

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.domain.ui.model.ModernThemePreset
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.Preference

/**
 * Always eligible on a version upgrade: Lalapo build numbers may skip individual values.
 * A consumed legacy key cannot override a later explicit DesignStyle selection.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class ModernAppearanceMigration(
    private val uiPreferences: UiPreferences,
) : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        migrateLegacyModernAppearance(
            legacy = uiPreferences.legacyModernAppearance,
            designStyle = uiPreferences.designStyle,
            modernPreset = uiPreferences.modernThemePreset,
        )
        return true
    }
}

/** Pure preference-level migration, shared with deterministic unit coverage. */
internal fun migrateLegacyModernAppearance(
    legacy: Preference<Boolean>,
    designStyle: Preference<DesignStyle>,
    modernPreset: Preference<ModernThemePreset>,
) {
    if (!legacy.isSet()) return

    if (!designStyle.isSet()) {
        val modern = legacy.get()
        designStyle.set(if (modern) DesignStyle.MODERN else DesignStyle.CLASSIC)
        if (modern && !modernPreset.isSet()) {
            modernPreset.set(ModernThemePreset.LALAPO)
        }
    }

    // Delete last so a partially interrupted migration is safe to retry.
    legacy.delete()
}
