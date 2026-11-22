plugins { id("loa.android.feature") }
android { namespace = "org.libraryofarts.feature.taste" }
dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
}
