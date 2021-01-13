@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package coreLibrary.lib

/**
 * 配置Api
 * 用于定义脚本的配置项
 * 配置项可在文件中或者使用指令修改
 * @sample
 * val welcomeMsg by config.key("Hello Steve","The message show when player join")
 * println(welcomeMsg)
 */
import cf.wayzer.script_agent.IBaseScript
import cf.wayzer.script_agent.events.ScriptDisableEvent
import cf.wayzer.script_agent.getContextModule
import cf.wayzer.script_agent.listenTo
import cf.wayzer.script_agent.util.DSLBuilder
import com.typesafe.config.*
import io.github.config4k.ClassContainer
import io.github.config4k.TypeReference
import io.github.config4k.readers.SelectReader
import io.github.config4k.toConfig
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.KProperty

open class ConfigBuilder(private val path: String) {
    /**
     * @param desc only display the first line using command
     */
    data class ConfigKey<T : Any>(val path: String, val cls: ClassContainer, val default: T, val desc: List<String>) {
        private lateinit var cache: T
        private var cacheTime = 0L
        private var onChange: ((T) -> Unit)? = null
        private fun cache(v: T): T {
            val changed = cacheTime == 0L || cache != v
            cache = v
            cacheTime = System.currentTimeMillis()
            if (changed)
                onChange?.invoke(v)
            return v
        }

        fun get(): T {
            if (cacheTime > lastLoad) return cache
            val v = fileConfig.extract(cls, path) ?: return cache(default)
            @Suppress("UNCHECKED_CAST")
            if (cls.mapperClass.isInstance(v))
                return cache(v as T)
            error("Wrong config type: $path get $v")
        }

        fun set(v: T) {
            fileConfig = fileConfig.withValue(
                path,
                v.toConfigValue().withOrigin(ConfigOriginFactory.newSimple().withComments(desc))
            )
            cache(v)
            saveFile()
        }

        /**
         * 清除设定值
         */
        fun reset() {
            if (!fileConfig.hasPath(path)) return
            fileConfig = fileConfig.withoutPath(path)
            saveFile()
        }

        /**
         * 写入默认值到文件中
         */
        fun writeDefault() {
            set(default)
        }

        fun getString(): String {
            return get().toConfig(path).getValue(path).render()
        }

        /**
         * @return format like [getString]
         * @throws IllegalArgumentException when parse fail
         */
        fun setString(strV: String): String {
            val str = "$path = $strV"
            val v = ConfigFactory.parseString(str).extract(cls, path)
            if (cls.mapperClass.isInstance(v)) {
                @Suppress("UNCHECKED_CAST")
                set(v as T)
                return str
            }
            throw IllegalArgumentException("Parse \"$str\" fail: get $v")
        }

        /**
         * add hook when value change, and when first time.
         */
        fun onChange(body: (T) -> Unit): ConfigKey<T> {
            onChange = body
            return this
        }

        operator fun getValue(thisRef: Any?, prop: KProperty<*>) = get()
        operator fun setValue(thisRef: Any?, prop: KProperty<*>, v: T) = set(v)

        companion object {
            /**
             * Copy from config4k as can't use reified param
             */
            fun Config.extract(cls: ClassContainer, path: String): Any? {
                if (!hasPath(path)) return null
                return SelectReader.getReader(cls).invoke(this, path)
            }

            fun Any.toConfigValue(): ConfigValue {
                return if (this is Map<*, *> && this.keys.all { it is String }) {//修复issue #7
                    ConfigValueFactory.fromMap(this.mapKeys {
                        it.key as String
                    }.mapValues { it.value?.toConfigValue() })
                } else this.toConfig("root").root()["root"]!!
            }
        }
    }

    fun child(sub: String) = ConfigBuilder("$path.$sub")
    fun <T : Any> key(cls: ClassContainer, default: T, vararg desc: String) =
        DSLBuilder.Companion.ProvideDelegate<IBaseScript, ConfigKey<T>> { script, name ->
            val key = ConfigKey("$path.$name", cls, default, desc.toList())
            script.configs.add(key)
            all[key.path] = key
            return@ProvideDelegate key
        }

    inline fun <reified T : Any> key(
        default: T,
        vararg desc: String
    ): DSLBuilder.Companion.ProvideDelegate<IBaseScript, ConfigKey<T>> {
        val genericType = object : TypeReference<T>() {}.genericType()
        return key(ClassContainer(T::class, genericType), default, *desc)
    }

    companion object {
        private val key_configs = DSLBuilder.DataKeyWithDefault("configs") { mutableSetOf<ConfigKey<*>>() }
        val IBaseScript.configs by key_configs
        val all = mutableMapOf<String, ConfigKey<*>>()
        var configFile: File = cf.wayzer.script_agent.Config.dataDirectory.resolve("config.conf")
        private lateinit var fileConfig: Config
        private var lastLoad: Long = -1

        init {
            ConfigBuilder::class.java.getContextModule()!!.listenTo<ScriptDisableEvent>(2) {
                key_configs.apply {
                    script.get()?.forEach { all.remove(it.path) }
                }
            }
            reloadFile()
        }

        fun reloadFile() {
            fileConfig = ConfigFactory.parseFile(configFile)
            lastLoad = System.currentTimeMillis()
            all.values.forEach {
                try {
                    it.get()
                } catch (e: Exception) {
                    Logger.getLogger("ConfigApi").log(Level.WARNING, "Fail to parse config ${it.path}", e)
                }
            }
        }

        fun saveFile() {
            configFile.writeText(fileConfig.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
        }
    }
}

val globalConfig = ConfigBuilder("global")
val IBaseScript.config get() = ConfigBuilder(id.replace('/', '.'))