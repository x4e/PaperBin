package dev.binclub.paperbin

import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.concurrent.thread
import kotlin.reflect.KProperty

/**
 * @author cookiedragon234 29/Apr/2020
 */
object PaperBinConfig {
	val saveFile = File("paperbin.properties")
	val properties = Properties()
	
	var antiChunkBan: Boolean by BooleanProperty(properties, "antiChunkBan", false)
	var antiCrasher: Boolean by BooleanProperty(properties, "antiCrasher")
	var antiDupe: Boolean by BooleanProperty(properties, "antiDupe")
	var antiElytraFly: Boolean by BooleanProperty(properties, "antiElytraFly")
	var antiGrief: Boolean by BooleanProperty(properties, "antiGrief")
	var antiIllegalItem: Boolean by BooleanProperty(properties, "antiIllegalItem", false)
	var antiNetherRoof: Boolean by BooleanProperty(properties, "antiNetherRoof")
	var antiNetherRoofLevel: Int by IntProperty(properties, "antiNetherRoof.level", 125)
	var antiNewChunks: Boolean by BooleanProperty(properties, "antiNewChunks", false)
	var antiPhysicsCrash: Boolean by BooleanProperty(properties, "antiPhysicsCrash", false)
	var antiWorldBorder: Boolean by BooleanProperty(properties, "antiWorldBorder", false)
	var physicsMaxStackSize: Int by IntProperty(properties, "antiPhysicsCrash.maxStackSize", 500)
	var antiPortalGodmode: Boolean by BooleanProperty(properties, "antiPortalGodmode")
	var antiUnicodeChat: Boolean by BooleanProperty(properties, "antiUnicodeChat", false)
	var blockRateLimit: Boolean by BooleanProperty(properties, "blockRateLimit")
	var chunkLoadOptimisations: Boolean by BooleanProperty(properties, "chunkLoadOptimisations", false)
	var customServerBrand: String by StringProperty(properties, "customServerBrand", "PaperBin")
	var fastGameRule: Boolean by BooleanProperty(properties, "fastGameRule")
	var elytraLowerRenderDistance: Boolean by BooleanProperty(properties, "elytraLowerRenderDistance", false)
	var elytraLowerRenderDistanceOverworld: Int by IntProperty(properties, "elytraLowerRenderDistanceOverworld", 6)
	var elytraLowerRenderDistanceNether: Int by IntProperty(properties, "elytraLowerRenderDistanceNether", 4)
	var elytraLowerRenderDistanceEnd: Int by IntProperty(properties, "elytraLowerRenderDistanceEnd", 8)
	var elytraLowerRenderDistanceFlying: Int by IntProperty(properties, "elytraLowerRenderDistanceFlying", 3)
	var elytraLowerRenderDistanceFlyingThresold: Double by DoubleProperty(properties, "elytraLowerRenderDistanceFlyingThresold", 1.05)
	var tpsCompensation: Boolean by BooleanProperty(properties, "tpsCompensation")
	var mobAiRateLimit: Boolean by BooleanProperty(properties, "mobAiRateLimit")
	var mobAiMultithreading: Boolean by BooleanProperty(properties, "mobAiMultithreading", false)
	var optimisedEveryoneSleeping: Boolean by BooleanProperty(properties, "optimisedEveryoneSleeping")
	var packetOptimisations: Boolean by BooleanProperty(properties, "packetOptimisations")
	var villageRateLimit: Boolean by BooleanProperty(properties, "villageRateLimit")
	var lightUpdateRateLimit: Boolean by BooleanProperty(properties, "lightUpdateRateLimit", true)
	var lightUpdateRateLimitDelay: Double by DoubleProperty(properties, "lightUpdateRateLimit.delay", 1000.0) // update light every 1 second
	var debug: Boolean by BooleanProperty(properties, "debug", false)
	
	init {
		load()
		save()
		//Runtime.getRuntime().addShutdownHook(thread (start = false, isDaemon = false) { this.save() })
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
}

class BooleanProperty(val properties: Properties, val key: String, default: Boolean = true) {
	init {
		properties.getProperty(key) ?: properties.setProperty(key, default.toString())
	}
	
	operator fun getValue(thisRef: Any?, prop: KProperty<*>): Boolean = java.lang.Boolean.parseBoolean(properties.getProperty(key))
	operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: Boolean) = properties.setProperty(key, value.toString())
}

class DoubleProperty(val properties: Properties, val key: String, default: Double = 0.0) {
	init {
		properties.getProperty(key) ?: properties.setProperty(key, default.toString())
	}
	
	operator fun getValue(thisRef: Any?, prop: KProperty<*>): Double = properties.getProperty(key).toDouble()
	operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: Double) = properties.setProperty(key, value.toString())
}

class IntProperty(val properties: Properties, val key: String, default: Int = 0) {
	init {
		properties.getProperty(key) ?: properties.setProperty(key, default.toString())
	}
	
	operator fun getValue(thisRef: Any?, prop: KProperty<*>): Int = properties.getProperty(key).toInt()
	operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: Int) = properties.setProperty(key, value.toString())
}

class StringProperty(val properties: Properties, val key: String, default: String = "") {
	init {
		properties.getProperty(key) ?: properties.setProperty(key, default)
	}

	operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = properties.getProperty(key)
	operator fun setValue(thisRef: Any?, prop: KProperty<*>, value: String) = properties.setProperty(key, value)
}
