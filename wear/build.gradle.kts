plugins {
    id("loa.android.application")
    id("loa.android.compose")
}

android {
    namespace = "org.libraryofarts.wear"
    defaultConfig {
        applicationId = "org.libraryofarts.wear"
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear)
    implementation(libs.kotlinx.coroutines.android)
}
