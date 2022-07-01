@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

import cf.wayzer.scriptAgent.emit
import coreLibrary.lib.PermissionApi.Global
import coreLibrary.lib.PermissionApi.PermissionHandler
import coreLibrary.lib.event.RequestPermissionEvent
import java.util.*

/**
 * 权限系统Api
 * 架构:
 *   coreLib: 抽象接口[PermissionHandler] -> 全局String权限解析[Global]
 *   - 工具类 [StringPermissionHandler] [PermissionGroup]
 *   coreLib/permissionCommand: 指令及持久化实现
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
        val allKnownSubject: Set<T> get() = emptySet()
    }

    /**
     * 全局权限处理器
     * fallback 组"@default"
     */
    companion object Global : PermissionHandler<List<String>> {
        val default = StringPermissionHandler()
        val handlers = LinkedList<PermissionHandler<String>>().apply {
            addLast(default)
        }
        val allKnownGroup: Set<String> get() = handlers.flatMapTo(mutableSetOf()) { it.allKnownSubject }

        fun handleGroup(group: String, permission: String): Result {
            return handlers.fold(Result.Default) { it, handler ->
                it.fallback { handler.handle(group, permission) }
            }
        }

        override fun List<String>.invoke(permission: String): Result {
            return fold(Result.Default) { r, g -> r.fallback { handleGroup(g, permission) } }
                .fallback { handleGroup("@default", permission) }
        }


        fun registerDefault(vararg permission: String, group: String = "@default") {
            default.registerPermission(group, permission.asIterable())
        }

        fun <T : Any> handleThoughEvent(
            subject: T, permission: String,
            defaultGroup: List<String> = emptyList()
        ): Result {
            val event = RequestPermissionEvent(subject, permission, defaultGroup).emit()
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
    class StringPermissionHandler : PermissionHandler<String> {
        val groups = mutableMapOf<String, PermissionGroup.Mutable>()
        override val allKnownSubject: Set<String> get() = groups.keys

        override fun String.invoke(permission: String): Result {
            return groups[this]?.run {
                extend.fold(get(permission)) { r, g ->
                    r.fallback { Global.handleGroup(g, permission) }
                }
            } ?: Result.Default
        }

        fun registerPermission(subject: String, permission: Iterable<String>) {
            groups.getOrPut(subject, PermissionGroup::Mutable)
                .add(PermissionGroup(permission.map(PermissionGroup::convertToInternal)))
        }

        fun unRegisterPermission(subject: String, permission: Iterable<String>) {
            groups[subject]?.apply {
                remove(PermissionGroup(permission.map(PermissionGroup::convertToInternal)))
                if (isEmpty()) groups.remove(subject)
            }
        }

        fun clear() {
            groups.clear()
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    open class PermissionGroup(
        open val has: Set<String>,
        open val reject: Set<String>,
        open val extend: List<String>
    ) {
        class Mutable(
            override val has: MutableSet<String> = mutableSetOf(),
            override val reject: MutableSet<String> = mutableSetOf(),
            override val extend: MutableList<String> = mutableListOf()
        ) : PermissionGroup(has, reject, extend) {
            fun add(group: PermissionGroup) {
                has += group.has
                reject += group.reject
                extend += group.extend
            }

            fun remove(group: PermissionGroup) {
                has -= group.has
                reject -= group.reject
                extend -= group.extend.toSet()
            }
        }

        /**@param nodes @开头为组, $结尾表示绝对权限,否则为通配权限 */
        constructor(nodes: List<String>) : this(
            nodes.asSequence()
                .filter { it[0] != '-' }.toSet(),
            nodes.asSequence()
                .filter { it[0] == '-' }
                .map { it.substring(1) }.toSet(),
            nodes.filter { it[0] == '@' }
        )

        fun get(node: String): Result {
            val prefix = allPrefix(node)
            return when {
                prefix.any { it in reject } -> Result.Reject
                prefix.any { it in has } -> Result.Has
                else -> Result.Default
            }
        }

        fun allNodes(): List<String> = buildList {
            addAll(has.asSequence().map(::convertFromInternal))
            addAll(reject.asSequence().map(::convertFromInternal).map { "-$it" })
        }

        fun isEmpty() = has.isEmpty() && reject.isEmpty() //extend included in has


        companion object {
            fun convertToInternal(permission: String): String {
                return when {
                    permission.startsWith("@") -> permission
                    permission.endsWith(".*") -> permission.removeSuffix(".*")
                    else -> "$permission$"
                }
            }

            fun convertFromInternal(permission: String): String {
                return when {
                    permission.startsWith("@") -> permission
                    permission.endsWith("$") -> permission.removeSuffix("$")
                    else -> "$permission.*"
                }
            }

            /**
             * Input a.b.c
             * Output "a.b.c$" "a.b.c" "a.b" "a"
             */
            fun allPrefix(node: String) = buildList {
                add("$node$")
                var index = node.length
                while (true) {
                    add(node.substring(0, index))
                    index = node.lastIndexOf('.', index - 1)
                    if (index < 0) break
                }
            }
        }
    }
}