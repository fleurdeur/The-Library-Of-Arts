package org.libraryofarts.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        when {
            pluginManager.hasPlugin("com.android.application") ->
                extensions.getByType<ApplicationExtension>().buildFeatures.compose = true

            pluginManager.hasPlugin("com.android.library") ->
                extensions.getByType<LibraryExtension>().buildFeatures.compose = true
        }

        dependencies {
            val bom = platform(lib("androidx-compose-bom"))
            add("implementation", bom)
            add("androidTestImplementation", bom)
            add("implementation", lib("androidx-compose-ui"))
            add("implementation", lib("androidx-compose-ui-graphics"))
            add("implementation", lib("androidx-compose-ui-tooling-preview"))
            add("implementation", lib("androidx-compose-foundation"))
            add("implementation", lib("androidx-compose-material3"))
            add("implementation", lib("androidx-compose-material-icons-extended"))
            add("debugImplementation", lib("androidx-compose-ui-tooling"))
            add("debugImplementation", lib("androidx-compose-ui-test-manifest"))
            add("androidTestImplementation", lib("androidx-compose-ui-test-junit4"))
        }
    }
}
