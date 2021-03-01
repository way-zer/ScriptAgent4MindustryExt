package mirai.lib

import coreLibrary.lib.PermissionApi.*
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User


/**
 * 若一个人拥有所有群中调用权限,请给与"{permission}.*"
 * 单群权限"{permission}.group{groupId}" 私聊权限 "{permission}.private"
 * 若让一个群中所有人可用，可以给"@default"对于权限
 */
object PermissionExt : HandlerWithFallback<User?>() {
    @Deprecated("use other handle", ReplaceWith("handle(subject,permission)"), DeprecationLevel.HIDDEN)
    override fun handle(subject: User?, permission: String): Result {
        return Global.handle(subject?.run { "qq${subject.id}" } ?: "@default", permission)
    }

    @JvmName("handleNotNull")
    fun handle(subject: User, permission: String): Result {
        val groupId = (subject as? Member)?.group?.run { "group$id" } ?: "private"
        return subject.invoke("$permission.$groupId")
    }

    fun handle(subject: Group, permission: String): Result {
        return null.invoke("$permission.group${subject.id}")
    }
}

fun User.hasPermission(permission: String) = PermissionExt.handle(this, permission).has
fun Group.hasPermission(permission: String) = PermissionExt.handle(this, permission).has