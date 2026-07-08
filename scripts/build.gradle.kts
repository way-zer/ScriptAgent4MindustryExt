import buildsrc.defineMetadataModules
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

defineMetadataModules()

dependencies {
    "coreMindustryApi"("com.github.TinyLake.MindustryX:core")
    configurations.findByName("coreBukkitApi")?.invoke("dev.folia:folia-api")
}

// 全局编译器配置
allprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs = listOf(
                "-Xinline-classes",
                "-opt-in=kotlin.RequiresOptIn",
                "-Xnullability-annotations=@arc.util:strict",
                "-Xcontext-parameters",
            )
        }
    }
}
