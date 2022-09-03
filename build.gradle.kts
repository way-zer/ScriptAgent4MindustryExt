import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("me.qoomon.git-versioning") version "2.1.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "cf.wayzer"
version = "v3.x.x" //采用3位版本号v1.2.3 1为大版本 2为插件版本 3为脚本版本

if (projectDir.resolve(".git").isDirectory)
    gitVersioning.apply(closureOf<me.qoomon.gradle.gitversioning.GitVersioningPluginConfig> {
        tag(closureOf<me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.VersionDescription> {
            pattern = "v(?<tagVersion>[0-9].*)"
            versionFormat = "\${tagVersion}"
        })
        commit(closureOf<me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.CommitVersionDescription> {
            versionFormat = "\${commit.short}-SNAPSHOT"
        })
    })

sourceSets {
    main {
        java.srcDir("scripts")
        java.exclude("cache")
    }
    create("plugin") {
        java.srcDir("plugin/src")
        resources.srcDir("plugin/res")
    }
}

repositories {
    mavenLocal()
    if (System.getProperty("user.timezone") == "Asia/Shanghai") {
//        maven(url = "https://maven.aliyun.com/repository/public")
    }
    mavenCentral()
    if (System.getProperty("user.timezone") != "Asia/Shanghai")//ScriptAgent
        maven("https://maven.wayzer.workers.dev/")
    else {
        maven {
            url = uri("https://packages.aliyun.com/maven/repository/2102713-release-0NVzQH/")
            credentials {
                username = "609f6fb4aa6381038e01fdee"
                password = "h(7NRbbUWYrN"
            }
        }
    }
    maven(url = "https://www.jitpack.io") {
        content {
            excludeModule("cf.wayzer", "ScriptAgent")
        }
    }
}

dependencies {
    val libraryVersion = "1.9.1.1"
    val mindustryVersion = "v136"
    val pluginImplementation by configurations
    pluginImplementation("cf.wayzer:ScriptAgent:$libraryVersion")
    pluginImplementation("cf.wayzer:LibraryManager:1.4.1")
    pluginImplementation("com.github.Anuken.Mindustry:core:$mindustryVersion")

    val implementation by configurations
    implementation(kotlin("script-runtime"))
    implementation("cf.wayzer:ScriptAgent:$libraryVersion")
    val kotlinScriptDef by configurations
    kotlinScriptDef("cf.wayzer:ScriptAgent:$libraryVersion")

    //coreLibrary
    implementation("cf.wayzer:PlaceHoldLib:5.2")
    implementation("io.github.config4k:config4k:0.4.1")
    //coreLib/DBApi
    val exposedVersion = "0.37.3"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    //coreMindustry
    implementation("com.github.Anuken.Mindustry:core:$mindustryVersion")
//    implementation("com.github.TinyLake.MindustryX:core:v138.001")
    //coreMindustry/console
    implementation("org.jline:jline-terminal:3.19.0")
    implementation("org.jline:jline-reader:3.19.0")
    //coreMindustry/utilContentsOverwrite
    implementation("cf.wayzer:ContentsTweaker:v2.0.1")

    //mirai
    implementation("net.mamoe:mirai-core-api-jvm:2.10.0")
    //wayzer
    implementation("com.google.guava:guava:30.1-jre")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf(
            "-Xinline-classes",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
    withType<ProcessResources> {
        inputs.property("version", rootProject.version)
        filter(
            filterType = org.apache.tools.ant.filters.ReplaceTokens::class,
            properties = mapOf("tokens" to mapOf("version" to rootProject.version))
        )
    }
    named<Delete>("clean") {
        delete(files("scripts/cache"))
    }
    create<Zip>("scriptsZip") {
        group = "plugin"
        from(sourceSets.main.get().allSource) {
            exclude("cache")
            exclude(".metadata")
        }
        archiveClassifier.set("scripts")
        doLast {
            println(archiveFile.get())
        }
    }
    val buildPlugin = create<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("buildPlugin") {
        group = "plugin"
        dependsOn("scriptsZip")
        from(sourceSets.getByName("plugin").output)
        archiveClassifier.set("")
        archiveVersion.set(rootProject.version.toString().substringBeforeLast('.'))
        configurations = listOf(project.configurations.getByName("pluginCompileClasspath"))
        manifest.attributes(
            "Main-Class" to "cf.wayzer.scriptAgent.GenerateMain"
        )
        dependencies {
            include(dependency("cf.wayzer:ScriptAgent"))
            include(dependency("cf.wayzer:LibraryManager"))
        }
        doLast {
            println(archiveFile.get())
        }
    }
    create<JavaExec>("precompile") {
        group = "plugin"
        dependsOn("buildPlugin")
        inputs.files(sourceSets.main.get().allSource)
        classpath(buildPlugin.outputs.files)
        mainClass.set("cf.wayzer.scriptAgent.GenerateMain")
        if (javaVersion >= JavaVersion.VERSION_16)
            jvmArgs("--add-opens java.base/java.net=ALL-UNNAMED")
    }
    create<Zip>("precompileZip") {
        group = "plugin"
        dependsOn("precompile")
        from(file("scripts/cache")) {
            include("**/*.ktc")
        }
        from(file("scripts")) {
            include("data/*")
            include("*/res/*")
        }
        archiveClassifier.set("precompile")
        doLast {
            println(archiveFile.get())
        }
    }
}