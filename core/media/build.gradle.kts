plugins {
    id("loa.android.library")
    id("loa.android.hilt")
}

android {
    namespace = "org.libraryofarts.core.media"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
