package coreLib.extApi

import org.h2.mvstore.MVMap
import org.h2.mvstore.type.DataType
import org.h2.mvstore.type.StringDataType

//@file:Depends("coreLibrary/extApi/KVStore")
interface KVStore {
    fun <V> open(name: String, type: DataType<V>) = open(name, type, StringDataType.INSTANCE)
    fun <K, V> open(name: String, key: DataType<K>, type: DataType<V>): MVMap<K, V>
}