package org.libraryofarts.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("loa.android.library")
        pluginManager.apply("loa.android.compose")
        pluginManager.apply("loa.android.hilt")

        dependencies {
            add("implementation", project(":core:common"))
            add("implementation", project(":core:model"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:datastore"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))
            add("implementation", lib("androidx-lifecycle-runtime-compose"))
            add("implementation", lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", lib("hilt-navigation-compose"))
            add("implementation", lib("coil-compose"))
            add("implementation", lib("kotlinx-coroutines-android"))
        }
    }
}
