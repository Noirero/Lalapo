package mihon.core.migration.migrations

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.domain.ui.model.ModernThemePreset
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext

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
        val legacy = uiPreferences.legacyModernAppearance
        if (!legacy.isSet()) return true

        if (!uiPreferences.designStyle.isSet()) {
            val modern = legacy.get()
            uiPreferences.designStyle.set(if (modern) DesignStyle.MODERN else DesignStyle.CLASSIC)
            if (modern && !uiPreferences.modernThemePreset.isSet()) {
                uiPreferences.modernThemePreset.set(ModernThemePreset.LALAPO)
            }
        }

        // Delete last so a partially interrupted migration is safe to retry.
        legacy.delete()
        return true
    }
}
