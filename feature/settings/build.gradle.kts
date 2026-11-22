plugins { id("loa.android.feature") }
android { namespace = "org.libraryofarts.feature.settings" }
dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
}
