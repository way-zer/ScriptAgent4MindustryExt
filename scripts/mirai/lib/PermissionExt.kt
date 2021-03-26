package mirai.lib

import coreLibrary.lib.PermissionApi
import coreLibrary.lib.PermissionApi.Global.handle
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User


/**
 * 若一个人拥有所有群中调用权限,请给与"{permission}.*"
 * 单群权限"{permission}.group{groupId}" 私聊权限 "{permission}.private"
 * 若让一个群中所有人可用，可以给"@default"对应权限
 */

fun User.hasPermission(permission: String): Boolean {
    val groupId = (this as? Member)?.group?.run { "group$id" } ?: "private"
    return PermissionApi.handle(listOf("qq${id}"), "$permission.$groupId").has
}

fun Group.hasPermission(permission: String) = PermissionApi.handle(emptyList(), "$permission.group${id}").has