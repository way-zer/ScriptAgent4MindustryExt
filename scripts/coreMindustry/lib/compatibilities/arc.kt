@file:Suppress("PackageDirectoryMismatch")

package arc.struct

@Deprecated("因为5.0已经弃用,逐渐摆脱兼容层", ReplaceWith("Seq", "arc.struct.Seq"))
typealias Array<T> = Seq<T>