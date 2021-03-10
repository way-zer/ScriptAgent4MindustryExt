@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

import coreLibrary.lib.PermissionApi.Global
import coreLibrary.lib.PermissionApi.PermissionHandler
import coreLibrary.lib.event.RequestPermissionEvent

/**
 * 权限系统Api
 * 架构:
 *   coreLib: 抽象接口[PermissionHandler] -> 全局String权限解析[Global]
 *          工具类 [StringPermissionHandler] [StringPermissionTree]
 *   coreLib/permissionCommand: 指令及持久化实现[Global.impl]
 *   各子模块针对不同subject,代理至[Global.handleThoughEvent]解析
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
     */
    fun interface PermissionHandler<T> {
        fun T.invoke(permission: String): Result
    }

    /**
     * 全局权限处理器
     * fallback 组"@default"
     */
    companion object Global : PermissionHandler<List<String>> {
        private val default = StringPermissionHandler()
        var impl: StringPermissionHandler? = null
        val allKnownGroup: Set<String> get() = default.allKnownGroup + impl?.allKnownGroup.orEmpty()

        fun handleGroup(group: String, permission: String): Result {
            return (impl?.handle(group, permission) ?: Result.Default).fallback {
                default.handle(group, permission)
            }
        }

        override fun List<String>.invoke(permission: String): Result {
            return fold(Result.Default) { r, g -> r.fallback { handleGroup(g, permission) } }
                .fallback { handleGroup("@default", permission) }
        }


        fun registerDefault(subject: String = "@default", vararg permission: String) {
            default.registerPermission(subject, permission.asIterable())
        }

        fun <T : Any> handleThoughEvent(
            subject: T, permission: String,
            defaultGroup: List<String> = emptyList()
        ): Result {
            val event = RequestPermissionEvent(subject, permission, defaultGroup).apply { emit() }
            event.directReturn?.let { return it }
            return handle(event.group, permission)
        }

        fun <T> PermissionHandler<T>.handle(subject: T, permission: String): Result {
            return subject.invoke(permission)
        }
    }

    /**Use for implementing delegate and Global*/
    /**
     * 针对string主体的权限处理器实现
     * 支持查询“@group”或者其他用string表示的权限节点
     */
    class StringPermissionHandler() : PermissionHandler<String> {
        private val allGroup = mutableSetOf<String>()
        val allKnownGroup get() = allGroup as Set<String>
        private val tree = StringPermissionTree(::hasGroup)
        override fun String.invoke(permission: String): Result {
            return tree.hasPermission(this, convertToInternal(permission))
        }

        private fun hasGroup(subject: String, group: String): Boolean {
            return Global.handleGroup(subject, group).has
        }

        private fun convertToInternal(permission: String): String {
            return when {
                permission.startsWith("@") -> permission
                permission.endsWith(".*") -> permission.removeSuffix(".*")
                else -> "$permission$"
            }
        }

        fun registerPermission(subject: String, permission: Iterable<String>) {
            allGroup.add(subject)
            permission.forEach {
                val pp = convertToInternal(it)
                if (pp.startsWith("-"))
                    tree.setPermission(subject, pp.substring(1), Result.Reject)
                else
                    tree.setPermission(subject, pp, Result.Has)
            }
        }

        fun clear() {
            allGroup.clear()
            tree.clear()
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