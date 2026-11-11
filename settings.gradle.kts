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
        // SDK das Google Home APIs (não está em repositório público): decifrado pelo CI para esta pasta.
        maven { url = uri("homesdk") }
    }
}
rootProject.name = "Painel"
include(":app")
