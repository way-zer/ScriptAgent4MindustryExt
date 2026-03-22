@file:Suppress("unused")

package buildsrc

import java.io.File

/**
 * ScriptAgent Gradle Plugin - 模块信息
 * 
 * 从 metadata 文件中提取的顶层模块配置
 */
internal data class ModuleInfo(
    val id: String,
    val depends: List<String> = emptyList(),
    val apiDeps: List<String> = emptyList(),
    val implementationDeps: List<String> = emptyList(),
)

/**
 * 解析 ScriptAgent metadata 文件，聚合成顶层模块配置
 * 
 * metadata 格式：
 * ```
 * ID coreLibrary
 * +IMPORT MavenDependsSingle com.example:lib:1.0
 * +IMPORT MavenDepends com.example:impl:1.0
 * +DEPENDS otherModule
 * 
 * ID coreLibrary/subModule   <- 子脚本，依赖与导入会聚合到 coreLibrary
 * ...
 * ```
 */
private fun parseMetadataFile(file: File, moduleId: String): ModuleInfo? {
    if (!file.exists()) return null

    val lines = file.readLines()
    val depends = linkedSetOf<String>()
    val apiDeps = linkedSetOf<String>()
    val implementationDeps = linkedSetOf<String>()

    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith("+DEPENDS ") -> {
                val dep = trimmed.removePrefix("+DEPENDS ").trim().split(" ").firstOrNull()
                if (dep != null) depends.add(dep)
            }
            trimmed.startsWith("+IMPORT MavenDependsSingle ") -> {
                apiDeps.add(trimmed.removePrefix("+IMPORT MavenDependsSingle ").trim())
            }
            trimmed.startsWith("+IMPORT MavenDepends ") -> {
                // metadata 只描述脚本编译依赖，不可靠地区分 Gradle api/implementation。
                // 这里统一提升为 api，优先保证 IDE 与脚本编译的可见性。
                apiDeps.add(trimmed.removePrefix("+IMPORT MavenDepends ").trim())
            }
        }
    }

    return ModuleInfo(moduleId, depends.toList(), apiDeps.toList(), implementationDeps.toList())
}

/**
 * 扫描脚本目录下的所有 .metadata 文件。
 * `.metadata` 的模块 id 由其所在目录相对路径决定，例如：
 * - `scripts/coreLibrary/.metadata` -> `coreLibrary`
 * - `scripts/coreLibrary/db/.metadata` -> `coreLibrary/db`
 */
internal fun scanMetadataDir(scriptsDir: File): Map<String, ModuleInfo> {
    if (!scriptsDir.exists()) return emptyMap()

    return scriptsDir.walkTopDown()
        .filter { it.isFile && it.name == ".metadata" }
        .mapNotNull { file ->
            val moduleId = file.parentFile.relativeTo(scriptsDir).invariantSeparatorsPath
            if (moduleId.isBlank()) null else parseMetadataFile(file, moduleId)
        }
        .associateBy { it.id }
}
