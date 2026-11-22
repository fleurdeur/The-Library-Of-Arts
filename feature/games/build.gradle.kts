plugins { id("loa.android.feature") }
android { namespace = "org.libraryofarts.feature.games" }
dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
}
