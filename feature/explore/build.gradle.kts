plugins { id("loa.android.feature") }
android { namespace = "org.libraryofarts.feature.explore" }
dependencies {
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.playServicesLocation)
    implementation(project(":core:media"))
}
