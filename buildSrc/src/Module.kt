@file:Suppress("unused")

package buildsrc

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.project

@DslMarker
internal annotation class ModuleBuilderMarker

@ModuleBuilderMarker
internal class ModuleScope(val moduleId: String, val project: Project, val sourceSet: SourceSet) {
    val exposedConfigurationName get() = sourceSet.apiConfigurationName.replace("Api", "Exposed")

    fun dependsOnModule(module: String, project: Project = this.project) {
        val sourceSet = project.sourceSets.getByName(mapIdToSourceSetName(module))
        dependsOn(ModuleScope(module, project, sourceSet))
    }

    fun dependsOn(module: ModuleScope) {
        project.dependencies {
            add(
                sourceSet.apiConfigurationName,
                project(module.project.path, module.exposedConfigurationName)
            )
        }
    }

    fun api(dep: Any) = project.dependencies.add(sourceSet.apiConfigurationName, dep)
    fun implementation(dep: Any) = project.dependencies.add(sourceSet.implementationConfigurationName, dep)
}

private fun mapIdToSourceSetName(id: String) = id.replace('/', '.')

// Child directories with their own `.metadata` become standalone modules,
// so the parent source set should exclude them from compilation.
private fun Project.excludeNestedMetadataModules(moduleId: String, sourceSet: SourceSet) {
    val moduleDir = projectDir.resolve(moduleId)
    if (!moduleDir.isDirectory) return

    moduleDir.walkTopDown()
        .filter { it.isDirectory && it != moduleDir && it.resolve(".metadata").isFile }
        .forEach { childDir ->
            val relative = childDir.relativeTo(moduleDir).invariantSeparatorsPath
            sourceSet.java.exclude("$relative/**")
        }
}

// Create the backing SourceSet and base configurations for a script module.
internal fun Project.createModule(
    name: String,
    onlyLibrary: Boolean = false,
): ModuleScope {
    val sourceSet = sourceSets.create(mapIdToSourceSetName(name)) {
        if (!onlyLibrary) {
            java.srcDir(name)
            java.exclude("lib/**")
        }
        //独立设置成srcDir，这样可以自定义package，不需要包含lib前缀。
        java.srcDir("$name/lib")
    }
    excludeNestedMetadataModules(name, sourceSet)
    return ModuleScope(name, project, sourceSet).apply {
        configurations.create(exposedConfigurationName) {
            extendsFrom(configurations.getByName(sourceSet.apiConfigurationName))
            isCanBeConsumed = true
            isCanBeResolved = false
        }
        dependencies.apply {
            implementation(kotlin("script-runtime"))
            implementation(rootProject)
            add(exposedConfigurationName, sourceSet.output)
        }
    }
}

// Apply additional module wiring after all dependent SourceSets are available.
internal fun configureModule(scope: ModuleScope, body: ModuleScope.() -> Unit) {
    scope.body()
}

private val Project.sourceSets
    get() = project.extensions.getByType(
        SourceSetContainer::class.java
    )
