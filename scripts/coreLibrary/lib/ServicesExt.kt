package coreLibrary.lib

import cf.wayzer.scriptAgent.util.Services
import kotlin.properties.ReadOnlyProperty

fun <T> Services.Registry<T>.getOrNull(): T? = current().firstOrNull()
fun <T> Services.Registry<T>.get(): T = getOrNull() ?: error("No provider for ${serviceClass.canonicalName}")
val <T> Services.Registry<T>.provided: Boolean get() = current().isNotEmpty()

val <T> Services.Registry<T>.nullable get() = ReadOnlyProperty<Any?, T?> { _, _ -> getOrNull() }
val <T> Services.Registry<T>.notNull get() = ReadOnlyProperty<Any?, T> { _, _ -> get() }
val <T> Services.Registry<T>.all get() = ReadOnlyProperty<Any?, List<T>> { _, _ -> current() }