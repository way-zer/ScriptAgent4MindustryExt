plugins {
    kotlin("jvm") version "1.3.70"
    id("me.qoomon.git-versioning") version "2.1.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    application
}

group = "cf.wayzer"
version = "v1.x.x" //采用3位版本号v1.2.3 1为大版本 2为插件版本 3为脚本版本
val libraryVersion = "1.2.1"

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
    runtimeOnly(scriptsCompile)
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
    }
}
val scriptsZip by tasks.registering(Zip::class) {
    group = "application"
    from(sourceSets.getByName("scripts").allSource)
    archiveClassifier.set("scripts")
}
application {
    val debug = true
    mainClassName = "cf.wayzer.scriptAgent.MainKt"
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8",
        if (!debug) "" else "-agentlib:jdwp=transport=dt_socket,server=n,address=127.0.0.1:5005,suspend=y"
    )
}
tasks.create("showDependency") {
    val set = scriptsCompile.resolvedConfiguration.resolvedArtifacts.toSet()
    println(set.joinToString("\n") { it.id.componentIdentifier.displayName })
}