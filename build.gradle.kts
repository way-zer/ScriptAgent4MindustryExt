plugins {
    kotlin("jvm") version "1.3.70"
}

group = "cf.wayzer"
version = "v1.0.0"
val pluginVersion = "1.0.1"
val mindustryVersion = "v104"

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://www.jitpack.io")
    maven("https://dl.bintray.com/way-zer/maven")
}
sourceSets {
    main {
        java.srcDir("src")
    }
}
dependencies {
    implementation("cf.wayzer:ScriptAgent4Mindustry:$pluginVersion")
    implementation(kotlin("script-runtime"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.Anuken.Mindustry:core:$mindustryVersion")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}