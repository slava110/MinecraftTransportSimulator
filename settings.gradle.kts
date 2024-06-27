import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern;
import java.util.regex.Matcher;
pluginManagement {
    repositories {
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.architectury.dev/") }
        maven { url = uri("https://files.minecraftforge.net/maven/") }
        gradlePluginPortal()
    }
}

plugins{
    "kotlin-dsl"
}

rootProject.name = "Immersive Vehicles"
include("mccore")

// println("Enabled platforms:")
// ext.set("enabled_platforms","forge-1.20.1")
// (ext.get("enabled_platforms") as? String)
//     ?.split(',')
//     ?.map { platform ->
//         val platformTrimmed = platform.trim()
//         val module = ":mcinterface${ platformTrimmed.replace(".", "").replace("-", "") }"
//         include(module)
//         println("- $platformTrimmed ($module)")
//         platformTrimmed to module
//     }
//     .orEmpty()
//     .also { gradle.extra["platforms"] = it }



// This allows to have any mcinterface without changing settings


val platforms = mutableListOf(Pair("",""))
platforms.removeAt(0) // because first element is null

Files.walk(Paths.get("."),1)
    .filter { Files.isDirectory(it) }
    //TODO Find a way to change gradle version or forgegradle plugin for 1.12.2 version
    // because of     /mcinterfaceforge1122/build.gradle:7
    .filter{ it.toString() != "./mcinterfaceforge1122"}
    .filter {it.toString().matches(Regex(".\\/mcinterface.*"))} 
    .forEach {
        val module = it.toString().replace(".", "").replace("/", "")
        val dirName = it.toString().replace("./", "")

        // Assuming that version is not release candidate / snapshot / anything non usual
        val noDotsVersion = module.replace("mcinterfaceforge","").split("")
        val majorVersion = noDotsVersion[1].toString()
        val minorVersion = noDotsVersion[2].toString() + noDotsVersion[3].toString()
        val patchVersion = noDotsVersion[4].toString()

        val stringDottedVersion = "forge-" +majorVersion.toString() + "." + minorVersion.toString() + "." + patchVersion.toString()
        
        print("Found platform ")
        print(module)
        print(" for version ")
        println(stringDottedVersion)
        include(module)
        platforms.add(Pair(stringDottedVersion,":"+module))
        }

gradle.extra["platforms"] = platforms

