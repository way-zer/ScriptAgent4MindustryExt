package coreLibrary

import com.typesafe.config.Config
import io.github.config4k.ClassContainer
import io.github.config4k.CustomType
import io.github.config4k.registerCustomType
import io.github.config4k.toConfig
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

name = "基础变量注册"

registerVarForType<Instant>().apply {
    registerToString("转换为Date，参数格式同Date") { _, obj, arg ->
        SimpleDateFormat(arg ?: "MM-dd").format(obj)
    }
}

registerVarForType<Duration>().apply {
    registerToString("参数设定单位(天,时,分,秒,d,h,m,s,默认m)") { _, obj, arg ->
        val unit = when (arg?.get(0)?.lowercaseChar()) {
            'd', '天' -> ChronoUnit.DAYS
            'h', '小', '时' -> ChronoUnit.HOURS
            'm', '分' -> ChronoUnit.MINUTES
            's', '秒' -> ChronoUnit.SECONDS
            else -> ChronoUnit.MINUTES
        }
        "%.2f%s".format((obj.seconds.toDouble() / unit.duration.seconds), arg ?: "")
    }
}

@Suppress("PropertyName")
val NANO_PRE_SECOND = 1000_000_000L
fun Duration.toConfigString(): String {
    //Select the smallest unit output
    return when {
        (nano % 1000) != 0 -> (seconds * NANO_PRE_SECOND + nano).toString() + "ns"
        (nano % 1000_000) != 0 -> ((seconds * NANO_PRE_SECOND + nano) / 1000).toString() + "us"
        nano != 0 -> ((seconds * NANO_PRE_SECOND + nano) / 1000_000).toString() + "ms"
        (seconds % 60) != 0L -> seconds.toString() + "s"
        (seconds % (60 * 60)) != 0L -> (seconds / 60).toString() + "m"
        (seconds % (60 * 60 * 24)) != 0L -> (seconds / (60 * 60)).toString() + "h"
        else -> (seconds / 60).toString() + "d"
    }
}
registerCustomType(object : CustomType {
    override fun testParse(clazz: ClassContainer) = false
    override fun parse(clazz: ClassContainer, config: Config, name: String) = UnsupportedOperationException()
    override fun testToConfig(obj: Any) = obj is Duration
    override fun toConfig(obj: Any, name: String): Config {
        return (obj as Duration).toConfigString().toConfig(name)
    }
})