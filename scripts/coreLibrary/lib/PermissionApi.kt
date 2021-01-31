@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

/**
 * 基本架构:
 *  抽象接口
 *  全局处理实现(T=String)
 *    基本参照wayzer/permission，增加默认权限声明，对负权限的支持
 *  TODO 针对不同主体的实现(可以委托至全局)
 *  TODO 更新指令系统
 */

interface PermissionHandler<T> {
    enum class Result { Has, Reject, Default }

    fun handle(subject: T, permission: String): Result

    object Global : StringPermissionHandler() {
        var delegate: PermissionHandler<String>? = null
        override fun handle(subject: String, permission: String): Result {
            val result = delegate?.handle(subject, permission) ?: Result.Default
            if (result != Result.Default) return result
            return super.handle(subject, permission)
        }

        fun registerDefault(subject: String, vararg permission: String) {
            registerPermission(subject, permission.asIterable())
        }
    }

    /**Use for implementing delegate and Global*/
    open class StringPermissionHandler : PermissionHandler<String> {
        protected val tree = StringPermissionTree()
        override fun handle(subject: String, permission: String): Result {
            return tree.hasPermission(subject, convertToInternal(permission))
        }

        protected fun convertToInternal(permission: String): String {
            return when {
                permission.startsWith("@") -> permission
                permission.endsWith(".*") -> permission.removeSuffix(".*")
                else -> "$permission$"
            }
        }

        protected fun registerPermission(subject: String, permission: Iterable<String>) {
            permission.forEach {
                val pp = convertToInternal(it)
                if (pp.startsWith("-"))
                    tree.setPermission(subject, pp.substring(1), Result.Reject)
                else
                    tree.setPermission(subject, pp, Result.Has)
            }
        }
    }

    /**Use for implementing StringPermissionHandler*/
    class StringPermissionTree {
        val map = mutableMapOf<String, StringPermissionTree>()
        val allow = mutableSetOf<String>()
        val reject = mutableSetOf<String>()
        private fun Set<String>.match(subject: String): Boolean {
            return any {
                it == subject || (it.startsWith("@") && Global.handle(subject, it) == Result.Has)
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
                map.getOrPut(sp[0], ::StringPermissionTree).setPermission(subject, sp.getOrNull(1) ?: "", result)
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