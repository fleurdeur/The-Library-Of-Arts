plugins { id("loa.android.feature") }
android { namespace = "org.libraryofarts.feature.learn" }
dependencies {
    implementation(project(":core:media"))
    implementation(libs.androidx.lifecycle.runtime.compose)
}
