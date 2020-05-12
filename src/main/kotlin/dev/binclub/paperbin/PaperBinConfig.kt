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
	
	init {
		Runtime.getRuntime().addShutdownHook(thread (start = false, isDaemon = false) {
			properties.store(saveFile.writer(), "Paper Bin Configuration")
		})
	}
	
	val properties = Properties().also {
		try {
			it.load(saveFile.reader())
		} catch (t: Throwable) {}
	}
	
	var antiCrasher: Boolean by BooleanProperty(properties, "antiCrasher")
	var antiDupe: Boolean by BooleanProperty(properties, "antiDupe")
	var antiNetherRoof: Boolean by BooleanProperty(properties, "antiNetherRoof")
	var blockRateLimit: Boolean by BooleanProperty(properties, "blockRateLimit")
	var fastGameRule: Boolean by BooleanProperty(properties, "fastGameRule")
	var foodTpsCompensate: Boolean by BooleanProperty(properties, "foodTpsCompensate")
	var mobAiRateLimit: Boolean by BooleanProperty(properties, "mobAiRateLimit")
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
