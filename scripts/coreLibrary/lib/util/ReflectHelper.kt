package coreLibrary.lib.util

import cf.wayzer.scriptAgent.util.DSLBuilder
import java.lang.reflect.Field
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ReflectDelegate<T, R>(
    private val field: Field, private val cls: Class<R>
) : ReadWriteProperty<T?, R> {
    override fun getValue(thisRef: T?, property: KProperty<*>): R = cls.cast(field.get(thisRef))
    override fun setValue(thisRef: T?, property: KProperty<*>, value: R) = field.set(thisRef, value)
}

inline fun <reified T, reified R> reflectDelegate() = DSLBuilder.nameGet { name ->
    val field = T::class.java.getDeclaredField(name)
    if (!field.isAccessible) field.isAccessible = true
    ReflectDelegate<T, R>(field, R::class.java)
}