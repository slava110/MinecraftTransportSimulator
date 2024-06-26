@file:Suppress("UnstableApiUsage")


plugins {
    id("dev.architectury.loom") version "1.6-SNAPSHOT"
}
repositories {
    mavenCentral()
    maven {
        name = "ParchmentMC"
        setUrl("https://maven.parchmentmc.org")
    }
    maven {
        name = "BlameJared"
        setUrl("https://maven.blamejared.com")
        content {
            includeGroup("mezz.jei")
        }
    }
    maven { url = uri("https://files.minecraftforge.net/maven/") }
    maven { url = uri("https://maven.architectury.dev/") }
}



val mc_version: String by project
val forge_version: String by project

val mod_version: String by project
val mod_group: String by project
val archive_name: String by project
val mod_id: String by project

base.archivesName.set(archive_name)
version = "${mc_version}-${mod_version}"
group = mod_group

val generatedResources = file("src/generated")




loom {
    silentMojangMappingsLicense()

    forge {
        convertAccessWideners = true
        mixinConfig("$mod_id.mixins.json")
    }

    runs {
        named("client") {
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            ideConfigGenerated(true)
            runDir("runServer")
        }
    }
}
val embed: Configuration by configurations.creating
    dependencies {
        minecraft("com.mojang:minecraft:${mc_version}")
        mappings(loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-1.20.1:2023.09.03@zip")
        })
        forge("net.minecraftforge:forge:${forge_version}")

        "embed"(project(":mccore"))

        annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.6")
        compileOnly("io.github.llamalad7:mixinextras-common:0.3.6")
        implementation("io.github.llamalad7:mixinextras-forge:0.3.6")
        include("io.github.llamalad7:mixinextras-forge:0.3.6")

        compileOnlyApi("mezz.jei:jei-1.20.1-common-api:15.3.0.8")
        compileOnlyApi("mezz.jei:jei-1.20.1-forge-api:15.3.0.8")
        runtimeOnly("mezz.jei:jei-1.20.1-forge:15.3.0.8")
    }




configurations.implementation {
    extendsFrom(embed)
}

sourceSets {
    main {
        resources.srcDir(generatedResources)
    }
}



tasks.jar {
    from(embed.map { if(it.isDirectory) it else zipTree(it) })
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(arrayOf("-Xlint:deprecation", "-Xlint:unchecked"))
    options.encoding = "UTF-8"
}