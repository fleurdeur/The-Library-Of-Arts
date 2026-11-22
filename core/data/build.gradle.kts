plugins {
    id("loa.android.library")
    id("loa.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.libraryofarts.core.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.okhttp)
    implementation(libs.hilt.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.sqlcipher)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
