ext.set("enabled_platforms", "1.20.1")

pluginManagement {
    repositories {
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://files.minecraftforge.net/maven/") }
        maven { url = uri("https://maven.architectury.dev/") }
        gradlePluginPortal()
    }
}

rootProject.name = "Immersive Vehicles"
include("mccore")

println("Enabled platforms:")


(ext.get("enabled_platforms") as? String)
    ?.split(',')
    ?.map { platform ->
        
        println("PL Module")
        val platformTrimmed = platform.trim()
        val module = ":mcinterfaceforge${ platformTrimmed.replace(".", "").replace("-", "") }"
        
        println(module)
        include(module)
        println("- $platformTrimmed ($module)")
        platformTrimmed to module
    }
    .orEmpty()
    .also { gradle.extra["platforms"] = it }