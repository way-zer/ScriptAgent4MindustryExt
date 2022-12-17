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
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.events.ScriptStateChangeEvent
import cf.wayzer.scriptAgent.getContextScript
import cf.wayzer.scriptAgent.listenTo
import cf.wayzer.scriptAgent.util.DSLBuilder
import com.typesafe.config.*
import io.github.config4k.ClassContainer
import io.github.config4k.TypeReference
import io.github.config4k.readers.SelectReader
import io.github.config4k.toConfig
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.KProperty

open class ConfigBuilder(private val path: String, val script: Script?) {
    /**
     * @param desc only display the first line using command
     */
    data class ConfigKey<T : Any>(
        val path: String,
        val cls: ClassContainer,
        val default: T,
        val desc: List<String>,
        private val onChange: ((T) -> Unit)?
    ) {
        private lateinit var cache: T
        private var cacheTime = 0L
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
            cache(v)
            modifyFile(path, v.toConfigValue().withOrigin(ConfigOriginFactory.newSimple().withComments(desc)))
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
            return get().toConfigValue().render(renderConfig)
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

    fun child(sub: String) = ConfigBuilder("$path.$sub", script)

    //internal
    fun <T : Any> key(
        script: Script, name: String,
        cls: ClassContainer, default: T, vararg desc: String,
        onChange: ((T) -> Unit)?
    ): ConfigKey<T> {
        val key = ConfigKey("$path.$name", cls, default, desc.toList(), onChange)
        script.configs.add(key)
        all[key.path] = key
        if (onChange != null) key.get()//ensure onChange get the init value
        return key
    }

    /**
     * The most commonly used api
     * Example(in script)
     * val port by config.key(8080,"示例配置项")
     */
    inline fun <reified T : Any> key(default: T, vararg desc: String) =
        DSLBuilder.Companion.ProvideDelegate<Any?, ConfigKey<T>> { obj, name ->
            val script: Script = when {
                obj is Script -> obj
                this.script != null -> this.script
                else -> error("Can't get script in context")
            }
            key(script, name, ClassContainer<T>(), default, *desc, onChange = null)
        }

    /**
     * commonly only use [onChange] not return
     * @param onChange hook when value change, and when first time.
     */
    inline fun <reified T : Any> key(
        name: String, default: T, vararg desc: String,
        noinline onChange: (T) -> Unit
    ): ConfigKey<T> {
        return key(
            script ?: error("Can't get script in context"), name,
            ClassContainer<T>(), default, *desc, onChange = onChange
        )
    }

    companion object {
        private val renderConfig = ConfigRenderOptions.defaults().setOriginComments(false)
        private val key_configs = DSLBuilder.DataKeyWithDefault("configs") { mutableSetOf<ConfigKey<*>>() }
        val Script.configs by key_configs
        val all = mutableMapOf<String, ConfigKey<*>>()
        var configBaseFile: File = cf.wayzer.scriptAgent.Config.dataDir.resolve("config.base.conf")
        var configFile: File = cf.wayzer.scriptAgent.Config.dataDir.resolve("config.conf")
        private lateinit var fileConfig: Config
        private lateinit var rawConfig: Config
        fun setRawConfig(raw: Config) {
            rawConfig = raw
            val base =
                if (configBaseFile.exists()) ConfigFactory.parseFile(configBaseFile) else ConfigFactory.empty()
            fileConfig = raw
                .withFallback(ConfigFactory.systemProperties())
                .withFallback(ConfigFactory.systemEnvironment())
                .withFallback(base)
        }

        private var lastLoad: Long = -1

        init {
            ConfigBuilder::class.java.getContextScript().listenTo<ScriptStateChangeEvent> {
                //when unload
                if (script.scriptState.loaded && !next.loaded)
                    key_configs.apply {
                        script.inst?.get()?.forEach { all.remove(it.path) }
                    }
            }
            reloadFile()
        }

        fun reloadFile() {
            setRawConfig(ConfigFactory.parseFile(configFile))
            lastLoad = System.currentTimeMillis()
            all.values.forEach {
                try {
                    it.get()
                } catch (e: Exception) {
                    Logger.getLogger("ConfigApi").log(Level.WARNING, "Fail to parse config ${it.path}", e)
                }
            }
        }

        fun modifyFile(path: String, value: ConfigValue?, save: Boolean = true) {
            setRawConfig(rawConfig.run {
                if (value != null) withValue(path, value) else withoutPath(path)
            })
            if (save) saveFile()
        }

        fun saveFile() {
            configFile.writeText(rawConfig.root().render(renderConfig))
        }

        inline fun <reified T : Any> ClassContainer(): ClassContainer {
            val genericType = object : TypeReference<T>() {}.genericType()
            return ClassContainer(T::class, genericType)
        }
    }
}

val globalConfig = ConfigBuilder("global", null)
val Script.config get() = ConfigBuilder(id.replace('/', '.'), this)