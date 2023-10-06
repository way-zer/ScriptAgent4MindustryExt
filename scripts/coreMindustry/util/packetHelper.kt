package coreMindustry.util

import arc.util.io.Reads
import arc.util.io.ReusableByteInStream
import arc.util.io.ReusableByteOutStream
import arc.util.io.Writes
import mindustry.gen.Building
import mindustry.gen.Call
import java.io.DataInputStream
import java.io.DataOutputStream

object PacketHelper {
    class Buffer {
        private val outStream = ReusableByteOutStream()
        private val inStream = ReusableByteInStream()
        val writes = Writes(DataOutputStream(outStream))
        val reads = Reads(DataInputStream(inStream))

        val size get() = outStream.size()

        fun flushBytes(): ByteArray {
            writes.close()
            val res = outStream.toByteArray()
            outStream.reset()
            return res
        }

        fun flushReads(): Reads {
            writes.close()
            inStream.setBytes(outStream.bytes, 0, outStream.size())
            outStream.reset()
            return reads
        }
    }

    fun syncTile(builds: List<Building>) {
        val dataBuffer = Buffer()
        var sent = 0
        builds.forEach {
            sent++
            dataBuffer.writes.i(it.pos())
            dataBuffer.writes.s(it.block.id.toInt())
            it.writeAll(dataBuffer.writes)
            if (dataBuffer.size > 800) {
                Call.blockSnapshot(sent.toShort(), dataBuffer.flushBytes())
                sent = 0
            }
        }
        if (sent > 0) {
            Call.blockSnapshot(sent.toShort(), dataBuffer.flushBytes())
        }
    }
}