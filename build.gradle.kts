plugins {
    kotlin("jvm") version "2.1.10"
    id("me.qoomon.git-versioning") version "6.4.4"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "cf.wayzer"
version = "v3.x.x" //采用3位版本号v1.2.3 1为大版本 2为插件版本 3为脚本版本
val loaderVersion get() = version.toString()

if (projectDir.resolve(".git").isDirectory)
    gitVersioning.apply {
        refs{
            tag("v(?<version>[0-9].*)"){
                version = "\${ref.version}"
            }
        }
        rev {
            version = "\${commit.short}"
        }
    }

sourceSets {
    main {
        java.srcDir("loader/src")
        resources.srcDir("loader/res")
    }
}


dependencies {
    val libraryVersion = "1.11.2.1"
    val mindustryVersion = "ca40f700fb" //v146.004
    api("cf.wayzer:ScriptAgent:$libraryVersion")
    implementation("cf.wayzer:LibraryManager:1.6")
    compileOnly("com.github.TinyLake.MindustryX:core:$mindustryVersion")

    subprojects {
        apply(plugin = "kotlin")
        dependencies {
            api(rootProject)
            api(kotlin("script-runtime"))
            kotlinScriptDef(rootProject)
        }
    }
}

kotlin {
    jvmToolchain(17)
}
tasks {
    allprojects {
        withType<JavaCompile>().configureEach {
            enabled = false
        }
    }
    named<Delete>("clean") {
        delete(files("scripts/cache"))
    }
    register<Zip>("scriptsZip") {
        group = "plugin"
        from("scripts") {
            include("bootStrap/**")
            include("coreLibrary/**")
            include("coreMindustry/**")
            include("wayzer/**")
            include("mapScript/**")
        }
        archiveClassifier.set("scripts")
        doLast {
            println(archiveFile.get())
        }
    }
    val buildPlugin by registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        group = "plugin"
        dependsOn("scriptsZip")
        from(sourceSets.main.map { it.output })
        archiveClassifier.set("")
        archiveVersion.set(loaderVersion)
        configurations = listOf(project.configurations.runtimeClasspath.get())
        manifest.attributes(
            "Main-Class" to "cf.wayzer.scriptAgent.standalone.LoaderKt"
        )
        dependencies {
            include(dependency("cf.wayzer:ScriptAgent"))
            include(dependency("cf.wayzer:LibraryManager"))
        }
        doLast {
            println(archiveFile.get())
        }
    }
    val destPacked = layout.buildDirectory.file("tmp/builtin.packed.zip")
    val precompile = register<JavaExec>("precompile") {
        dependsOn(buildPlugin)
        group = "plugin"
        classpath(buildPlugin.map { it.outputs.files })
        systemProperties["ScriptAgent.PreparePack"] = "true"
        environment("SAMain", "bootStrap/generate")

        inputs.files(sourceSets.main.get().allSource)
        outputs.file(destPacked)
    }
    val precompileZip = register<Zip>("precompileZip") {
        dependsOn(precompile)
        group = "plugin"
        archiveClassifier.set("precompile.packed")

        from(zipTree(destPacked))
        doLast {
            println(archiveFile.get())
        }
    }

    register<Jar>("allInOneJar") {
        dependsOn(buildPlugin, precompileZip)
        group = "plugin"
        archiveClassifier.set("allInOne")
        includeEmptyDirs = false

        from(buildPlugin.map { zipTree(it.outputs.files.singleFile) })
        from(zipTree(destPacked)) {
            into("builtin")
        }
    }
}
