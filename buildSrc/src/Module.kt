@file:Suppress("unused")

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.project

class ModuleScope(val moduleId: String, private val project: Project, private val sourceSet: SourceSet) {
    fun DependencyHandler.dependsModule(module: String, project: Project = this@ModuleScope.project) =
        add(sourceSet.apiConfigurationName, project(project.path, module + "Exposed"))

    fun DependencyHandler.api(dep: Any) = add(sourceSet.apiConfigurationName, dep)
    fun DependencyHandler.implementation(dep: Any) = add(sourceSet.implementationConfigurationName, dep)
}

fun Project.defineModule(
    name: String,
    srcDir: String = name,
    body: ModuleScope.() -> Unit,
) {
    val sourceSet = sourceSets.create(name) {
        java.srcDir(srcDir)
    }
    val exposed = configurations.create(name + "Exposed") {
        extendsFrom(configurations.getByName(name + "Api"))
        isCanBeConsumed = true
    }
    dependencies.apply {
        add(exposed.name, sourceSet.output)
    }
    ModuleScope(name, project, sourceSet).apply {
        dependencies.apply {
            api(project("::scripts"))
        }
        body()
    }
}

private val Project.sourceSets
    get() = project.extensions.getByType(
        SourceSetContainer::class.java
    )