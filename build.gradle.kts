plugins {
    kotlin("jvm") version "1.4.0"
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
        this.delete += fileTree("scripts/cache")
    }
    create<Zip>("scriptsZip") {
        group = "application"
        from(sourceSets.main.get().allSource) {
            exclude("*.ktc")
            exclude("cache.jar")
            exclude(".metadata")
        }
        archiveClassifier.set("scripts")
    }
    create<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("buildApplication") {
        dependsOn("scriptsZip")
        group = "application"
        from(sourceSets.getByName("plugin").output)
        archiveClassifier.set("")
        archiveVersion.set(rootProject.version.toString().substringBeforeLast('.'))
        configurations = listOf(project.configurations.getByName("pluginCompileClasspath"))
        dependencies {
            include(dependency("cf.wayzer:ScriptAgent"))
            include(dependency("cf.wayzer:LibraryManager"))
        }
        manifest {
            attributes("Main-Class" to "cf.wayzer.scriptAgent.LoaderKt")
        }
    }
}