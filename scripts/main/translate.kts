package translate

import arc.*
import arc.util.*
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import main.*
import mindustry.game.EventType.*
import mindustry.gen.*
import mindustry.mod.*
import java.io.ByteArrayOutputStream
import java.lang.String.*
import java.math.BigInteger
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.atomic.AtomicReference


class Translate {
    init {
        val fanyi = BaiduFanyi.create()
            .appId("20220803001292897")
            .securityCode("kPz01NWNLPA2cvBAV_UT")
        Log.info("Loaded private info.")

        //listen for game load event
        listen<EventType.PlayerChatEvent> { e ->
            val from = e.player.locale
            val to = if (from.equals("en",true)) "zh" else "en"
            val translate: String? = fanyi.translate(e.message, from, to)
            Call.sendMessage(e.message)
            Call.sendMessage("[sky][TR][yellow][" + e.player.name + "][sky]: " + translate)
        }
    }

    @Override
    fun loadContent() {
        Log.info("Loading some example content.")
    }


}

class BaiduFanyi {
    lateinit var appId: String
    lateinit var securityCode: String
    val ROOT: String = "https://fanyi-api.baidu.com/api/trans/vip/translate"

    fun appId(appid: String): BaiduFanyi {
        appId = appid
        return this
    }

    fun securityCode(code: String): BaiduFanyi {
        securityCode = code
        return this
    }

    /**
     * @param query 要翻译的文字
     * @param from  原语言
     * @param to    翻译后的语言
     * @return 翻译后的文字
     */
    fun translate(query: String, from: String, to: String): String? {
        val salt: String = (1000000000 until 9999999999).random().toString()
        val sign: String = md5(appId + query + salt + securityCode).toString()
        val jStr: String = doGet(ROOT + "?q=" + query + "&from=" + from + "&to=" + to + "&appid=" + appId + "&salt=" + salt + "&sign=" + sign).toString()
        val reader = JsonReader()
        val json = reader.parse(jStr)
        val result = AtomicReference("null")
        json.forEach { child: JsonValue ->
            if (child.name().equals("error_msg", ignoreCase = true)) {
                result.set(child.asString())
            } else {
                if (child.name().equals("trans_result", ignoreCase = true)) {
                    if (child.size > 0) {
                        result.set(child[0].getString("dst"))
                    }
                }
            }
        }
        return result.get()
    }

    /**
     * @param query 要翻译的文字
     * @param to    翻译后的语言
     * @return 翻译后的文字
     */
    fun translate(query: String, to: String): String? {
        return translate(query, "auto", to)
    }

    private fun random(size: Int): String? {
        val src = "abcdefghijklmnopqrstuvwxyz"
        val zd: Array<String> =
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

    private fun md5(text: String): String? {
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

    private fun doGet(s: String): String? {
        return try {
            val url = URL(s)
            val conn = url.openConnection()
            val bos = ByteArrayOutputStream()
            val inputS = conn.getInputStream()
            var i = 0
            while (inputS.read().also { i = it } != -1) {
                bos.write(i)
            }
            inputS.close()
            bos.toString("UTF-8")
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

    companion object {
        fun create(): BaiduFanyi {
            return BaiduFanyi()
        }
    }
}

