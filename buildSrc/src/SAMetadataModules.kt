package buildsrc

import org.gradle.api.Project
import java.io.File

// Resolve script-level dependencies like `coreLibrary/extApi/KVStore` to the
// nearest folder that actually declares a `.metadata` module.
private fun resolveModuleDependency(dependencyId: String, moduleIds: Set<String>): String? {
    var current = dependencyId
    while (current.isNotEmpty()) {
        if (current in moduleIds) return current
        current = current.substringBeforeLast('/', "")
    }
    return null
}

// Register modules in two phases: create all source sets first, then wire dependencies.
fun Project.defineMetadataModules(scriptsDir: File = projectDir) {
    val modules = scanMetadataDir(scriptsDir)
    val moduleIds = modules.keys
    val scopes = linkedMapOf<String, ModuleScope>()

    for (info in modules.values.sortedBy { it.id }) {
        scopes[info.id] = createModule(info.id)
    }

    for (info in modules.values.sortedBy { it.id }) {
        val scope = scopes.getValue(info.id)
        configureModule(scope) {
            for (dep in info.depends.mapNotNull { resolveModuleDependency(it, moduleIds) }.distinct()) {
                if (dep == info.id) continue
                dependsOnModule(dep)
            }
            for (dep in info.apiDeps) {
                api(dep)
            }
            for (dep in info.implementationDeps) {
                implementation(dep)
            }
        }
    }
}
