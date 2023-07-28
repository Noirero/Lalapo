plugins {
    id("com.android.library")
    kotlin("android")
    // AM (CU) -->
    kotlin("plugin.serialization")
    // <-- AM (CU)
    id("com.squareup.sqldelight")
}

android {
    namespace = "tachiyomi.data"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    sqldelight {
        database("AnimeDatabase") {
            packageName = "tachiyomi.mi.data"
            dialect = "sqlite:3.24"
            sourceFolders = listOf("sqldelightanime")
        }
    }
}

dependencies {
    implementation(project(":source-api"))
    implementation(project(":domain"))
    implementation(project(":core"))

    api(libs.sqldelight.android.driver)
    api(libs.sqldelight.coroutines)
    api(libs.sqldelight.android.paging)
}
// AM (CU) -->
tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xcontext-receivers",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}
// <-- AM (CU)
