plugins {
    `kotlin-dsl`
}

group = "org.libraryofarts.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "loa.android.application"
            implementationClass = "org.libraryofarts.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "loa.android.library"
            implementationClass = "org.libraryofarts.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "loa.android.compose"
            implementationClass = "org.libraryofarts.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "loa.android.hilt"
            implementationClass = "org.libraryofarts.buildlogic.AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "loa.android.feature"
            implementationClass = "org.libraryofarts.buildlogic.AndroidFeatureConventionPlugin"
        }
    }
}
