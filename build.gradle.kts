plugins {
    kotlin("jvm") version "1.3.72"
    id("me.qoomon.git-versioning") version "2.1.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    application
}

group = "cf.wayzer"
version = "v1.x.x" //采用3位版本号v1.2.3 1为大版本 2为插件版本 3为脚本版本
val libraryVersion = "1.2-7db5e01-DIRTY"

gitVersioning.apply(closureOf<me.qoomon.gradle.gitversioning.GitVersioningPluginConfig> {
    tag(closureOf<me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.VersionDescription> {
        pattern = "v(?<tagVersion>[0-9].*)"
        versionFormat = "\${tagVersion}"
    })
    commit(closureOf<me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.CommitVersionDescription> {
        versionFormat = "\${version}-\${commit.short}\${dirty}"
    })
})

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven(url = "https://www.jitpack.io")
    maven("https://dl.bintray.com/way-zer/maven")
}
val scriptsCompile = configurations.create("scriptsCompile")
sourceSets {
    create("scripts") {
//        compileClasspath+=configurations.getByName("compileOnly")
        java.srcDir("scripts")
    }
}

dependencies {
    api("cf.wayzer:ScriptAgent:$libraryVersion")
    api(kotlin("script-runtime"))
    api(kotlin("stdlib-jdk8"))
}

apply {
    from("scripts.gradle.kts")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<ProcessResources> {
        inputs.property("version", rootProject.version)
        filter(
            filterType = org.apache.tools.ant.filters.ReplaceTokens::class,
            properties = mapOf("tokens" to mapOf("version" to rootProject.version))
        )
    }
    named<Delete>("clean") {
        this.delete += fileTree("scripts").filter { it.name.endsWith(".cache.jar") }
        this.delete += fileTree("scripts").filter { it.name.endsWith(".ktc") }
    }
    create<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("buildApplication") {
        dependsOn("scriptsZip")
        group = "application"
        from(sourceSets.getByName("main").output)
        archiveClassifier.set("all")
        configurations = listOf(project.configurations.getByName("compileClasspath"))
        dependencies {
            include(dependency("cf.wayzer:LibraryManager"))
            include(dependency("cf.wayzer:ScriptAgent"))
        }
//        into("lib"){
//            from(configurations.first().files{ it.name=="ScriptAgent"}.also(::println))
//        }
        manifest {
            attributes("Main-Class" to "cf.wayzer.scriptAgent.LoaderKt")
        }
    }
}
val scriptsZip by tasks.registering(Zip::class) {
    group = "application"
    from(sourceSets.getByName("scripts").allSource) {
        exclude("*.ktc")
        exclude("cache.jar")
        exclude(".metadata")
    }
    archiveClassifier.set("scripts")
}
tasks.create("showDependency") {
    group = "application"
    val set = scriptsCompile.resolvedConfiguration.resolvedArtifacts.toSet()
    println(set.joinToString("\n") { it.id.componentIdentifier.displayName })
}