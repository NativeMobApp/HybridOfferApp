pluginManagement {
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
        gradlePluginPortal()
        maven {
            url = uri("file://C:/Users/gasto/AndroidStudioProjects/MyApplication/flutter_module_hybrid/build/host/outputs/repo")
        }
        maven {
            url = uri("https://storage.googleapis.com/download.flutter.io")
        }
    }

}

rootProject.name = "My Application"
include(":app")

include(":flutter_module_hybrid")
project(":flutter_module_hybrid").projectDir = file("flutter_module_hybrid")