package web.lib.serverList

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

object PingUtil {
    enum class GameMode {
        survival, sandbox, attack, pvp, editor
    }

    data class Info(
        val name: String,
        val address: String,
        val mapName: String,
        val players: Int,
        val wave: Int,
        val version: Int,
        val type: String,
        val mode: GameMode,
        val limit: Int,
        val description: String,
        val timeMs: Int,
        var online: Boolean,
        var lastUpdate: Long
    ) {
        constructor(address: String, timeMs: Int, buffer: ByteBuffer) : this(
            buffer.string, address, buffer.string, buffer.int, buffer.int, buffer.int,
            buffer.string, GameMode.values()[buffer.get().toInt()], buffer.int, buffer.string,
            timeMs, true, System.currentTimeMillis()
        )

        companion object {
            val ByteBuffer.string: String
                get() {
                    val length = get().toInt()
                    val bs = ByteArray(length)
                    get(bs)
                    return String(bs)
                }
        }
    }

    @Throws(IOException::class)
    fun ping(addressWithPort: String): Info {
        val sp = addressWithPort.split(':')
        val port = sp.getOrNull(1)?.toIntOrNull() ?: 6567
        val socket = DatagramSocket()
        socket.send(DatagramPacket(byteArrayOf(-2, 1), 2, InetAddress.getByName(sp[0]), port))
        socket.soTimeout = 2000
        val packet = DatagramPacket(ByteArray(512), 512)
        val start = System.currentTimeMillis()
        socket.receive(packet)
        val end = System.currentTimeMillis()
        return Info(addressWithPort.removeSuffix(":6567"), (end - start).toInt(), ByteBuffer.wrap(packet.data))
    }
}