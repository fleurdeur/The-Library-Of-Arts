pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TheGreatLibraryOfArts"

include(
    ":app",
    ":core:common",
    ":core:model",
    ":core:designsystem",
    ":core:database",
    ":core:network",
    ":core:datastore",
    ":core:data",
    ":core:media",
    ":core:ui",
    ":feature:home",
    ":feature:explore",
    ":feature:artwork",
    ":feature:journal",
    ":feature:taste",
    ":feature:collections",
    ":feature:learn",
    ":feature:games",
    ":feature:settings",
    ":wear",
    ":baselineprofile",
)
