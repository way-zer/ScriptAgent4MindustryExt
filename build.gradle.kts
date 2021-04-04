import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    id("me.qoomon.git-versioning") version "2.1.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "cf.wayzer"
version = "v1.x.x" //采用3位版本号v1.2.3 1为大版本 2为插件版本 3为脚本版本

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

apply {
    from("dependencies.gradle.kts")
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
    }
    create<JavaExec>("precompile") {
        group = "plugin"
        dependsOn("buildPlugin")
        inputs.files(sourceSets.main.get().allSource)
        classpath(buildPlugin.outputs.files)
        mainClass.set("cf.wayzer.scriptAgent.GenerateMain")
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
    }
}