pluginManagement {
    repositories {
        google()
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

rootProject.name = "notification_timer"

include(":app")
include(":feature:timers")
include(":feature:presets")
include(":feature:settings")
include(":core:ui")
include(":core:domain")
include(":core:data")
include(":core:runtime")
include(":core:testing")
