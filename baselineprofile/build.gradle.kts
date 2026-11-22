plugins {
    id("loa.android.library")
}

android {
    namespace = "org.libraryofarts.baselineprofile"
}

dependencies {
    androidTestImplementation(libs.androidx.benchmark.macro.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.junit)
}
