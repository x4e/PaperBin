package dev.binclub.paperbin

import java.io.File
import java.util.*
import kotlin.concurrent.thread
import kotlin.reflect.KProperty

/**
 * @author cookiedragon234 29/Apr/2020
 */
object PaperBinConfig {
	val saveFile = File("paperbin.properties")
	val properties = Properties()
	
	init {
		load()
		Runtime.getRuntime().addShutdownHook(thread (start = false, isDaemon = false) { this.save() })
	}
	
	fun save(): Boolean {
		try {
			properties.store(saveFile.writer(), "PaperBin Configuration")
			return true
		} catch (t: Throwable) {
			t.printStackTrace()
		}
		return false
	}
	
	fun load(): Boolean {
		try {
			properties.load(saveFile.reader())
			return true
		} catch (t: Throwable) {
			t.printStackTrace()
		}
		return false
	}
	
	var antiChunkBan: Boolean by BooleanProperty(properties, "antiChunkBan", false)
	var antiCrasher: Boolean by BooleanProperty(properties, "antiCrasher")
	var antiDupe: Boolean by BooleanProperty(properties, "antiDupe")
	var antiGrief: Boolean by BooleanProperty(properties, "antiGrief")
	var antiNetherRoof: Boolean by BooleanProperty(properties, "antiNetherRoof")
	var antiNewChunks: Boolean by BooleanProperty(properties, "antiNewChunks")
	var blockRateLimit: Boolean by BooleanProperty(properties, "blockRateLimit")
	var chunkLoadOptimisations: Boolean by BooleanProperty(properties, "chunkLoadOptimisations", false)
	var fastGameRule: Boolean by BooleanProperty(properties, "fastGameRule")
	var foodTpsCompensate: Boolean by BooleanProperty(properties, "foodTpsCompensate")
	var mobAiRateLimit: Boolean by BooleanProperty(properties, "mobAiRateLimit")
	var optimisedEveryoneSleeping: Boolean by BooleanProperty(properties, "optimisedEveryoneSleeping")
	var packetOptimisations: Boolean by BooleanProperty(properties, "packetOptimisations")
	var villageRateLimit: Boolean by BooleanProperty(properties, "villageRateLimit")
	var debug: Boolean by BooleanProperty(properties, "debug", false)
}

class BooleanProperty(val properties: Properties, val key: String, default: Boolean = true) {
	init {
		properties.getProperty(key) ?: properties.setProperty(key, default.toString())
	}
	
	operator fun getValue(thisRef: Any?, prop: KProperty<*>): Boolean = java.lang.Boolean.parseBoolean(properties.getProperty(key))
	operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: Boolean) = properties.setProperty(key, value.toString())
}
