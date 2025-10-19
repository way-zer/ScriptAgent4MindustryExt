@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

import cf.wayzer.scriptAgent.emitAsync
import coreLibrary.lib.PermissionApi.*
import coreLibrary.lib.event.RequestPermissionEvent
import java.io.Serializable
import java.time.Instant

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
    interface PermissionHandler<T> {
        fun findAll(subject: T): List<PermissionNode>
        fun find(subject: T, permission: String): Sequence<PermissionNode>
        val allKnownSubject: Set<T> get() = emptySet()
    }

    /**
     * 全局权限处理器
     * fallback 组"@default"
     */
    companion object Global : PermissionHandler<List<String>> {
        val default = StringPermissionHandler()

        object ByGroup : MutableList<PermissionHandler<String>> by mutableListOf(default),
            PermissionHandler<String> {
            override fun findAll(subject: String): List<PermissionNode> = flatMap { it.findAll(subject) }
            override fun find(
                subject: String,
                permission: String,
            ): Sequence<PermissionNode> = asSequence().flatMap { it.find(subject, permission) }

            override val allKnownSubject: Set<String> get() = flatMapTo(mutableSetOf()) { it.allKnownSubject }
        }

        override fun findAll(subject: List<String>): List<PermissionNode> = subject.flatMap { ByGroup.findAll(it) }
        override fun find(subject: List<String>, permission: String): Sequence<PermissionNode> = sequence {
            subject.forEach { yieldAll(ByGroup.find(it, permission)) }
            yieldAll(ByGroup.find("@default", permission))
        }

        val allKnownGroup: Set<String> get() = ByGroup.allKnownSubject

        fun registerDefault(vararg permission: String, group: String = "@default") {
            default.registerPermission(group, permission.asIterable())
        }

        suspend fun <T : Any> handleThoughEvent(
            subject: T, permission: String,
            defaultGroup: List<String> = emptyList()
        ): Result {
            val event = RequestPermissionEvent(subject, permission, defaultGroup).emitAsync()
            event.directReturn?.let { return it }
            return find(event.group, permission).query().asResult()
        }

        fun check(subject: List<String>, permission: String) = find(subject, permission).query()?.value ?: false

        fun findNode(map: Map<String, PermissionNode>, node: String) = sequence {
            map[node]?.let { yield(it) }
            var sp = node.lastIndexOf('.')
            while (sp > 0) {
                val prefix = node.substring(0, sp)
                map["$prefix.*"]?.let { yield(it) }
                sp = node.lastIndexOf('.', sp - 1)
            }
        }

        @Deprecated("use check", ReplaceWith(expression = "PermissionApi.check(subject)"))
        fun <T> PermissionHandler<T>.handle(subject: T, permission: String): Result {
            return find(subject, permission).query().asResult()
        }

        fun Sequence<PermissionNode>.query(
            time: Instant = Instant.now(),
            context: Set<String> = emptySet()
        ) = firstOrNull {
            (it.expire == null || it.expire.isAfter(time)) && context.containsAll(it.context)
        }

        fun PermissionNode?.asResult(): Result {
            val data = this ?: return Result.Default
            return if (data.value) Result.Has else Result.Reject
        }
    }

    /**Use for implementing delegate and Global*/
    /**
     * 针对string主体的权限处理器实现
     * 支持查询“@group”或者其他用string表示的权限节点
     */
    class StringPermissionHandler : PermissionHandler<String> {
        val groups = mutableMapOf<String, PermissionGroup>()
        override val allKnownSubject: Set<String> get() = groups.keys + groups.values.flatMap { it.extend }

        fun registerPermission(subject: String, permission: Iterable<String>) {
            groups.getOrPut(subject, ::PermissionGroup).apply {
                permission.forEach { add(it) }
            }
        }

        fun clear() {
            groups.clear()
        }

        override fun findAll(subject: String): List<PermissionNode> {
            val group = groups[subject] ?: return emptyList()
            return buildList {
                addAll(group.map.values)
                group.extend.forEach {
                    addAll(Global.ByGroup.findAll(it))
                }
            }
        }

        override fun find(subject: String, permission: String): Sequence<PermissionNode> {
            val group = groups[subject] ?: return emptySequence()
            return sequence {
                yieldAll(group.find(permission))
                group.extend.forEach {
                    yieldAll(Global.ByGroup.find(it, permission))
                }
            }
        }
    }

    data class PermissionNode(
        val name: String,
        val value: Boolean = true,
        val expire: Instant? = null,
        val context: List<String> = emptyList()
    ) : Serializable

    @Suppress("MemberVisibilityCanBePrivate")
    class PermissionGroup(
        val map: MutableMap<String, PermissionNode> = mutableMapOf(),
        val extend: MutableList<String> = mutableListOf(),
    ) {
        fun find(node: String) = Global.findNode(map, node)
        fun add(node: String) {
            if (node[0] == '@') {
                extend.add(node)
            } else if (node[0] == '-') {
                val name = node.substring(1)
                map[name] = PermissionNode(name, false)
            } else {
                map[node] = PermissionNode(node)
            }
        }

        fun allNodes(): List<String> = buildList {
            extend.forEach { add("@$it") }
            map.forEach { (k, v) ->
                add((if (!v.value) "-$k" else k))
            }
        }

        fun isEmpty() = map.isEmpty() && extend.isEmpty()
    }
}
