@file:Suppress("unused")

package coreLibrary.lib

/**
 * 可用做解析带变量的字符串
 * 也可用于脚本间共享数据或者接口
 * 暴露数据或接口时,注意类型所在的生命周期
 */

import cf.wayzer.placehold.*
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.PlaceHold.Updatable
import coreLibrary.lib.PlaceHold.dumbTemplateHandler
import kotlin.reflect.KProperty

typealias PlaceHoldString = PlaceHoldContext

object PlaceHold {
    fun interface Updatable<T> {
        fun update(v: T)
    }

    class PlaceHoldKey<T>(val name: String, private val cls: Class<T>) {
        operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
            val v = PlaceHoldApi.GlobalContext.getVar(name)
            if (cls.isInstance(v)) return cls.cast(v)
            error("Can't get globalVar: $name get $v")
        }
    }

    class TypePlaceHoldKey<R>(val name: String, private val cls: Class<R>) {
        operator fun <T : Any> getValue(thisRef: T, prop: KProperty<*>): R {
            val v = PlaceHoldApi.GlobalContext.resolveVar(thisRef, name)
            if (cls.isInstance(v)) return cls.cast(v)
            error("Can't get typeVar $name: FROM $thisRef GET $v")
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    class ScriptTypeBinder<T : Any>(
        private val script: Script,
        private val namePrefix: String,
        private val binder: TypeBinder<T>
    ) {
        fun registerToString(desc: String, body: DynamicVar<T, String>) =
            registerChildAny(PlaceHoldContext.ToString, desc, body)

        fun registerChild(key: String, desc: String, body: DynamicVar<T, Any>) = registerChildAny(key, desc, body)
        fun registerChildAny(key: String, desc: String, body: Any?) {
            script.registeredVars["$namePrefix.$key"] = desc
            script.onEnable { binder.registerChildAny(key, body) }
            if (body != null)
                script.onDisable { binder.registerChildAny(key, null) }
        }
    }

    // Map of name to description
    val Script.registeredVars by DSLBuilder.dataKeyWithDefault { mutableMapOf<String, String>() }

    /**
     * @param v support [cf.wayzer.placehold.DynamicVar] even [PlaceHoldString] or any value
     */
    fun register(script: Script, name: String, desc: String, v: Any?): Updatable<Any?> {
        script.registeredVars[name] = desc
        script.onEnable { PlaceHoldApi.registerGlobalVar(name, v) }
        script.onDisable { PlaceHoldApi.registerGlobalVar(name, null) }
        return Updatable { PlaceHoldApi.registerGlobalVar(name, it) }
    }

    /**
     * @see TypeBinder
     * @param desc describe what you want to add
     */
    @Deprecated("use registerForType(script): ScriptTypeBinder<T> instead")
    inline fun <reified T : Any> registerForType(script: Script, desc: String): TypeBinder<T> {
        script.registeredVars["Type@${T::class.java.simpleName}"] = desc
        return PlaceHoldApi.typeBinder()
    }

    /**
     * @see TypeBinder
     */
    inline fun <reified T : Any> registerForType(script: Script): ScriptTypeBinder<T> {
        return ScriptTypeBinder(script, "Type@${T::class.java.simpleName}", PlaceHoldApi.typeBinder())
    }

    /**
     * @sample
     * val tps by PlaceHold.reference<Int>("tps")
     * println(tps) //get variable
     */
    inline fun <reified T> reference(name: String) = PlaceHoldKey(name, T::class.java)

    /**
     * @sample
     * val Player.money by PlaceHold.referenceForType<Int>("money")
     * player.money //get variable
     */
    inline fun <reified R> referenceForType(name: String) = TypePlaceHoldKey(name, R::class.java)

    internal val dumbTemplateHandler = TemplateHandler { _, text -> text }
}

/**
 * @param arg values support [cf.wayzer.placehold.DynamicVar] even [PlaceHoldString] or any value
 */
fun String.with(vararg arg: Pair<String, Any>): PlaceHoldString = PlaceHoldApi.getContext(this, arg.toMap())
fun PlaceHoldString.with(vararg arg: Pair<String, Any>): PlaceHoldString =
    "".with(*arg).createChild(text, vars)

/** Convert String to PlaceHoldString with no PlaceHold and templateHandler */
fun String.asPlaceHoldString() = "{text}".with("text" to this, TemplateHandlerKey to dumbTemplateHandler)

/**
 * @see PlaceHold.register
 */
@ScriptDsl
fun Script.registerVar(name: String, desc: String, v: Any?) = PlaceHold.register(this, name, desc, v)

/**
 * @see PlaceHold.registerForType
 */
@Suppress("DEPRECATION")
@Deprecated("use registerVarForType() instead", ReplaceWith("IBaseScript.registerVarForType()"))
inline fun <reified T : Any> Script.registerVarForType(desc: String) = PlaceHold.registerForType<T>(this, desc)

/**
 * @see PlaceHold.registerForType
 */
@ScriptDsl
inline fun <reified T : Any> Script.registerVarForType() = PlaceHold.registerForType<T>(this)