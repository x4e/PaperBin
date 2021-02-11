package dev.binclub.paperbin.native

import dev.binclub.paperbin.native.NativeAccessor.OS.*
import java.io.File
import java.lang.reflect.Method


/**
 * @author cookiedragon234 22/Sep/2020
 */
object NativeAccessor {
	enum class OS {
		WINDOWS,
		LINUX,
		MAC;
		
		companion object {
			private val os: String = System.getProperty("os.name")
			fun get(): OS = when {
				os.contains("windows", true) -> WINDOWS
				os.contains("linux", true) -> LINUX
				os.contains("mac", true) -> MAC
				else -> error("Unsupported OS $os")
			}
		}
	}
	
	/*init {
		val os = OS.get()
		val prefix = when (os) {
			WINDOWS -> ""
			LINUX, MAC -> "lib"
		}
		val suffix = when(os) {
			WINDOWS -> "dll"
			LINUX -> "so"
			MAC -> "dylib"
		}
		
		val libraryFile = File(
			System.getenv("java.library.path"),
			"JvmClassHook.$suffix"
		)
		libraryFile.delete()
		libraryFile.deleteOnExit()
		
		val resourceName = "/native/${prefix}JvmClassHook.$suffix"
		val `is` = NativeAccessor::class.java.getResourceAsStream(resourceName)
			?: error("Couldn't find native library $resourceName")
		libraryFile.writeBytes(`is`.readBytes())
		
		System.load(libraryFile.absolutePath)
	}*/
	
	
	external fun registerClassLoadHook(hook: PaperBinClassTransformer)
	external fun appendToClassloader(
		url: String,
		bootloader: Boolean = false // System/Bootloader
	)
	external fun registerAntiPhysicsCrash(method: Method, maxStackSize: Int)
}

interface PaperBinClassTransformer {
	fun onClassLoad(
		clazz: Class<*>?,
		loader: ClassLoader?,
		className: String?,
		classfileBuffer: ByteArray
	): ByteArray?
	
	fun onClassPrepare(
		clazz: Class<*>
	)
}
