plugins { id("loa.android.feature") }
android { namespace = "org.libraryofarts.feature.artwork" }
dependencies {
    implementation(project(":core:media"))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.arCore)
}
