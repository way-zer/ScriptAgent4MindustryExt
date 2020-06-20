plugins {
    kotlin("jvm") version "1.3.70"
    id("me.qoomon.git-versioning") version "2.1.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "cf.wayzer"
version = "v1.x.x" //采用3位版本号v1.2.3 1为大版本 2为插件版本 3为脚本版本
val libraryVersion = "1.2.5"
//val libraryVersion = "1.1.5"
val mindustryVersion = "v104"

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
sourceSets {
    main {
        java.srcDir("src")
    }
    create("plugin") {
        this.compileClasspath += main.get().compileClasspath
        this.runtimeClasspath += main.get().runtimeClasspath
        java.srcDir("plugin/src")
        resources.srcDir("plugin/res")
    }
}
dependencies {
    api("cf.wayzer:ScriptAgent:$libraryVersion")
    implementation(kotlin("script-runtime"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.Anuken.Mindustry:core:$mindustryVersion")

    //coreLibrary
    api("cf.wayzer:PlaceHoldLib:2.1.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")
    implementation("com.h2database:h2-mvstore:1.4.200")
    implementation("org.jetbrains.exposed:exposed-core:0.24.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.24.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.24.1")
    implementation("io.github.config4k:config4k:0.4.1")
    //mirai
    implementation("net.mamoe:mirai-core:0.39.5")
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
        this.delete += fileTree("src").filter { it.name.endsWith(".cache.jar") }
        this.delete += fileTree("src").filter { it.name.endsWith(".ktc") }
    }
    create<Zip>("scriptsZip") {
        group = "plugin"
        from(sourceSets.main.get().allSource) {
            exclude("*.ktc")
            exclude("cache.jar")
            exclude(".metadata")
        }
        archiveClassifier.set("scripts")
    }
    create<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("buildPlugin") {
        dependsOn("scriptsZip")
        group = "plugin"
        from(sourceSets.getByName("plugin").output)
        archiveClassifier.set("")
        configurations = listOf(project.configurations.getByName("compileClasspath"))
        dependencies {
            include(dependency("cf.wayzer:ScriptAgent:$libraryVersion"))
            include(dependency("cf.wayzer:LibraryManager"))
        }
    }
}