Looking to report an issue/bug or make a feature request? Please refer to the [README file](https://github.com/aniyomiorg/aniyomi#issues-feature-requests-and-contributing).

---

Thanks for your interest in contributing to Animiru!


# Code contributions

Pull requests are welcome!

If you're interested in taking on [an open issue](https://github.com/Quickdesh/Animiru/issues), please comment on it so others are aware.
You do not need to ask for permission nor an assignment.

## Prerequisites

Before you start, please note that the ability to use following technologies is **required** and that existing contributors will not actively teach them to you.

- Basic [Android development](https://developer.android.com/)
- [Kotlin](https://kotlinlang.org/)

### Tools

- [Android Studio](https://developer.android.com/studio)
- Emulator or phone with developer options enabled to test changes.

## Keep In Mind

Do remember to mark each line of code that is either added or edited (excluding imports) specifying the reason behind the code.
Surround the new code with:

`AM (<Title of code>) --> <-- AM (<Title of code>)`

### Credits

- **AM (REMOVE_TABBED_SCREENS)** --> Refactoring from Aniyomi code to Animiru!
- **AM (REMOVE_ACRA_FIREBASE)** --> Refactoring from Aniyomi code to Animiru!
- **AM (FILLERMARK)** --> Thank you Quickdesh!
- **AM (BROWSE)** --> Thank you Quickdesh!
- **AM (KEYBOARD_CONTROLS)** --> Thank you Quickdesh!
- **AM (NAVIGATION_PILL)** --> Thank you Quickdesh!
- **AM (FILE_SIZE)** --> Thank you Khaled0!
- **AM (DISCORD_RPC)** --> Original library from dead8309/Kizzy, refactored code by 最高 man/Shivam. Thank you, both of you!
- **AM (CUSTOM_INFORMATION)** --> Copied from TachiyomiSY, Thank you jobobby4/syer!
- **AM (GROUPING)** --> Copied from TachiyomiSY, Thank you jobobby4/syer!
- **AM (RECENTS)** --> Idea inspired from TachiyomiJ2K, Thank you Jays2Kings!
- **AM (SYNC, SYNC_DRIVE, SYNC_YOMI)** --> Original code in SyncYomi, copied from Kuukiyomi. Thank you Kaiserbh and Luftverbot!


## Linting

To auto-fix some linting errors, run the `ktlintFormat` Gradle task.

## Getting help

- Join [the Discord server](https://discord.gg/F32UjdJZrR) for online help and to ask questions while developing.

# Translations

Translations are done externally via Weblate. See [our website](https://aniyomi.org/docs/contribute#translation) for more details.


# Forks

Forks are allowed so long as they abide by [the project's LICENSE](https://github.com/tachiyomiorg/tachiyomi/blob/master/LICENSE).

When creating a fork, remember to:

- To avoid confusion with the main app:
  - Change the app name
  - Change the app icon
  - Change or disable the [app update checker](https://github.com/Quickdesh/Animiru/blob/master/app/src/main/java/eu/kanade/tachiyomi/data/updater/AppUpdateChecker.kt)
- To avoid installation conflicts:
  - Change the `applicationId` in [`build.gradle.kts`](https://github.com/Quickdesh/Animiru/blob/master/app/build.gradle.kts)
- To avoid having your data polluting the main app's analytics and crash report services:
  - If you want to use Firebase analytics, replace [`google-services.json`](https://github.com/aniyomiorg/aniyomi/blob/master/app/src/standard/google-services.json) with your own
  - If you want to use ACRA crash reporting, replace the `ACRA_URI` endpoint in [`build.gradle.kts`](https://github.com/aniyomiorg/aniyomi/blob/master/app/build.gradle.kts) with your own
