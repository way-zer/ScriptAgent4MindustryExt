import buildsrc.defineMetadataModules
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

defineMetadataModules()

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
