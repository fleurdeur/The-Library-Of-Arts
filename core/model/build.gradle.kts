plugins {
    id("loa.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.libraryofarts.core.model"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
