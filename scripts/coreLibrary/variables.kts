package coreLibrary

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
        val unit = when (arg?.get(0)?.toLowerCase()) {
            'd', '天' -> ChronoUnit.DAYS
            'h', '小', '时' -> ChronoUnit.HOURS
            'm', '分' -> ChronoUnit.MINUTES
            's', '秒' -> ChronoUnit.SECONDS
            else -> ChronoUnit.MINUTES
        }
        "%.2f%s".format((obj.seconds.toDouble() / unit.duration.seconds), arg ?: "")
    }
}