package translate.baidu

import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

var appId = "20220803001292897"
var securityCode = "kPz01NWNLPA2cvBAV_UT"

/**
 * @param query 要翻译的文字
 * @param from  原语言
 * @param to    翻译后的语言
 * @return 翻译后的文字
 */
fun translate(query: String, from: String, to: String, action: Boolean): String {
    val salt = random(5)
    val sign = md5(appId + query + salt + securityCode)
    val queryE: String = try {
        URLEncoder.encode(query, "UTF-8")
    } catch (e: UnsupportedEncodingException) {
        throw RuntimeException(e)
    }
    val jStr =
        URL("https://fanyi-api.baidu.com/api/trans/vip/translate" + "?q=" + queryE + "&from=" + from + "&to=" + to + "&appid=" + appId + "&salt=" + salt + "&sign=" + sign + if (action) "&action=1" else "").readText()
    val reader = JsonReader()
    val json = reader.parse(jStr)
    val result = AtomicReference("null")
    json.forEach(Consumer { child: JsonValue ->
        if (child.name().equals("error_msg", ignoreCase = true)) {
            result.set(child.asString())
        } else {
            if (child.name().equals("trans_result", ignoreCase = true)) {
                if (child.size > 0) {
                    result.set(child[0].getString("dst"))
                }
            }
        }
    })
    return result.get()
}
fun Website(query: String, from: String, to: String, action: Boolean): String {
    val salt = random(5)
    val sign = md5(appId + query + salt + securityCode)
    val queryE: String = try {
        URLEncoder.encode(query, "UTF-8")
    } catch (e: UnsupportedEncodingException) {
        throw RuntimeException(e)
    }
    return "https://fanyi-api.baidu.com/api/trans/vip/translate" + "?q=" + queryE + "&from=" + from + "&to=" + to + "&appid=" + appId + "&salt=" + salt + "&sign=" + sign + if (action) "&action=1" else ""
}
/**
 * @param query 要翻译的文字
 * @param to    翻译后的语言
 * @return 翻译后的文字
 */
@JvmOverloads
fun translate(query: String, to: String, action: Boolean = false): String {
    return translate(query, "auto", to, action)
}
@JvmOverloads
fun Website(query: String, to: String, action: Boolean = false): String {
    return Website(query, "auto", to, action)
}

fun random(size: Int): String {
    val src = "abcdefghijklmnopqrstuvwxyz"
    val zd =
        (src.lowercase(Locale.getDefault()) + src.uppercase(Locale.getDefault()) + "0123456789").split("".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
    val random = Random()
    val sb = StringBuilder()
    for (i in 0 until size) {
        sb.append(zd[random.nextInt(zd.size)])
    }
    return sb.toString()
}

fun md5(text: String): String {
    return try {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(text.toByteArray(StandardCharsets.UTF_8))
        val data = digest.digest()
        val bigInteger = BigInteger(1, data)
        val result = StringBuilder(bigInteger.toString(16))
        while (result.length < 32) {
            result.insert(0, "0")
        }
        result.toString()
    } catch (e: Throwable) {
        throw RuntimeException(e)
    }
}

fun doGet(s: String): String {
    return try {
        val url = URL(s)
        val conn = url.openConnection()
        val bos = ByteArrayOutputStream()
        val `is` = conn.getInputStream()
        var i: Int
        while (`is`.read().also { i = it } != -1) {
            bos.write(i)
        }
        `is`.close()
        bos.toString("UTF-8")
    } catch (e: Throwable) {
        throw RuntimeException(e)
    }
}

