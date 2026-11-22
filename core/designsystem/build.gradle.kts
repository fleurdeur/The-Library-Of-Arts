plugins {
    id("loa.android.library")
    id("loa.android.compose")
}

android {
    namespace = "org.libraryofarts.core.designsystem"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
