plugins { id("loa.android.feature") }
android { namespace = "org.libraryofarts.feature.collections" }
dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.documentfile)
}
