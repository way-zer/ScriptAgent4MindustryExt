@file:Suppress("unused")

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.project

@DslMarker
annotation class ModuleBuilderMarker

@ModuleBuilderMarker
class ModuleScope(val moduleId: String, val project: Project, val sourceSet: SourceSet) {
    val exposedConfigurationName get() = sourceSet.apiConfigurationName.replace("Api", "Exposed")

    fun DependencyHandler.dependsOnModule(module: String, project: Project = this@ModuleScope.project): Dependency? {
        val sourceSet = project.sourceSets.getByName(mapIdToSourceSetName(module))
        return dependsOn(ModuleScope(module, project, sourceSet))
    }

    fun DependencyHandler.dependsOn(module: ModuleScope): Dependency? {
        return add(sourceSet.apiConfigurationName, project(module.project.path, module.exposedConfigurationName))
    }

    fun DependencyHandler.api(dep: Any) = add(sourceSet.apiConfigurationName, dep)
    fun DependencyHandler.implementation(dep: Any) = add(sourceSet.implementationConfigurationName, dep)
}

private fun mapIdToSourceSetName(id: String) = id.replace('/', '.')

@ModuleBuilderMarker
fun Project.defineModule(
    name: String,
    onlyLibrary: Boolean = false,
    body: ModuleScope.() -> Unit,
) {
    val sourceSet = sourceSets.create(mapIdToSourceSetName(name)) {
        if(!onlyLibrary){
            java.srcDir(name)
            java.exclude("lib/**")
        }
        //独立设置成srcDir，这样可以自定义package，不需要包含lib前缀。
        java.srcDir("$name/lib")
    }
    ModuleScope(name, project, sourceSet).apply {
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
        body()
    }
}

@ModuleBuilderMarker
fun ModuleScope.subModule(
    name: String,
    onlyLibrary: Boolean = false,
    body: ModuleScope.() -> Unit,
) {
    val parent = this@subModule
    val project = parent.project

    val id = "${parent.moduleId}/$name"
    parent.sourceSet.java.exclude("$name/**")
    project.dependencies {
        project.defineModule(id, onlyLibrary = onlyLibrary) {
            dependsOn(parent)
            body()
        }
    }
}

private val Project.sourceSets
    get() = project.extensions.getByType(
        SourceSetContainer::class.java
    )