@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

/**
 * 权限系统Api
 * 架构:
 *   coreLib: 抽象接口[PermissionHandler] -> 基础实现[HandlerWithFallback] -> 全局String权限解析[Global]
 *   coreLib/permissionCommand: 指令及持久化实现[Global.impl]
 *   各子模块针对不同subject的实现,代理至[Global]解析，并提供```fun T.hasPermission(permission:String)```实现
 * 提供fallback接口的目的: 便于其他模块扩充处理器的功能,例如wayzer/lib/PermissionExt
 */
interface PermissionApi {
    enum class Result {
        Has, Reject, Default;

        val has get() = this == Has

        /**
         * 帮助函数,用于串联多个权限处理器,具有优先级
         */
        fun fallback(body: () -> Result) = if (this == Default) body() else this
    }

    /**
     * 权限处理器抽象接口
     * 本接口主要用于支持lambda
     * 最好继承[HandlerWithFallback]
     */
    fun interface PermissionHandler<T> {
        fun T.invoke(permission: String): Result
    }

    /**
     * [fallback]用于没有其他结果时调用
     * should overwrite [handle]
     */
    abstract class HandlerWithFallback<T> : PermissionHandler<T> {
        var fallback = PermissionHandler<T> { Result.Default }
        abstract fun handle(subject: T, permission: String): Result
        override fun T.invoke(permission: String) = handle(this, permission)
            .fallback { fallback.handle(this, permission) }
    }
    /**Use for implementing delegate and Global*/
    /**
     * 针对string主体的权限处理器实现
     * 支持查询“@group”或者其他用string表示的权限节点
     */
    open class StringPermissionHandler : HandlerWithFallback<String>() {
        protected val allGroup = mutableSetOf<String>()
        open val allKnownGroup get() = allGroup as Set<String>
        protected val tree = StringPermissionTree(::hasGroup)
        override fun handle(subject: String, permission: String): Result {
            return tree.hasPermission(subject, convertToInternal(permission))
        }

        open fun hasGroup(subject: String, group: String): Boolean {
            return handle(subject, group).has
        }

        protected fun convertToInternal(permission: String): String {
            return when {
                permission.startsWith("@") -> permission
                permission.endsWith(".*") -> permission.removeSuffix(".*")
                else -> "$permission$"
            }
        }

        protected fun registerPermission(subject: String, permission: Iterable<String>) {
            allGroup.add(subject)
            permission.forEach {
                val pp = convertToInternal(it)
                if (pp.startsWith("-"))
                    tree.setPermission(subject, pp.substring(1), Result.Reject)
                else
                    tree.setPermission(subject, pp, Result.Has)
            }
        }

        protected fun clear() {
            allGroup.clear()
            tree.clear()
        }
    }

    /**
     * 全局权限处理器
     * fallback 组"@default"
     */
    companion object Global : StringPermissionHandler() {
        var impl: StringPermissionHandler? = null
        override val allKnownGroup: Set<String> get() = super.allKnownGroup + impl?.allKnownGroup.orEmpty()
        override fun handle(subject: String, permission: String): Result {
            val result = (impl?.handle(subject, permission) ?: Result.Default)
                .fallback { super.handle(subject, permission) }
            return if (subject == "@default") result
            else result.fallback { handle("@default", permission) }
        }

        fun registerDefault(subject: String = "@default", vararg permission: String) {
            registerPermission(subject, permission.asIterable())
        }

        fun <T> PermissionHandler<T>.handle(subject: T, permission: String): Result {
            return subject.invoke(permission)
        }
    }

    /**
     * Use for implementing [Global.impl]
     * @param hasGroup 由数根负责传递,树根为null
     */
    class StringPermissionTree(private val hasGroup: ((subject: String, group: String) -> Boolean)) {
        val test by lazy { }
        val map = mutableMapOf<String, StringPermissionTree>()
        val allow = mutableSetOf<String>()
        val reject = mutableSetOf<String>()
        private fun Set<String>.match(subject: String): Boolean {
            return any {
                it == subject || (it.startsWith("@") && hasGroup(subject, it))
            }
        }

        fun setPermission(subject: String, permission: String, result: Result) {
            if (permission.isEmpty()) {
                when (result) {
                    Result.Has -> {
                        allow.add(subject)
                        reject.remove(subject)
                    }
                    Result.Reject -> {
                        allow.remove(subject)
                        reject.add(subject)
                    }
                    Result.Default -> {
                        allow.remove(subject)
                        reject.remove(subject)
                    }
                }
            } else {
                val sp = permission.split('.', limit = 2)
                map.getOrPut(sp[0]) { StringPermissionTree(hasGroup) }
                    .setPermission(subject, sp.getOrNull(1) ?: "", result)
            }
        }

        fun hasPermission(subject: String, permission: String): Result {
            return if (permission.isEmpty()) {
                when {
                    reject.match(subject) -> Result.Reject
                    allow.match(subject) -> Result.Has
                    else -> Result.Default
                }
            } else {
                val sp = permission.split('.', limit = 2)
                map[sp[0]]?.hasPermission(subject, sp.getOrNull(1) ?: "") ?: Result.Default
            }
        }

        fun clear() {
            map.clear()
            allow.clear()
            reject.clear()
        }
    }
}