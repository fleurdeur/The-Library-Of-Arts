plugins { id("loa.android.feature") }
android { namespace = "org.libraryofarts.feature.journal" }
dependencies {
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
