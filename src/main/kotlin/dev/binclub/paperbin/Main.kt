package dev.binclub.paperbin

import dev.binclub.paperbin.PaperBinInfo.logger
import dev.binclub.paperbin.native.NativeAccessor
import net.minecraft.server.v1_12_R1.MinecraftServer
import org.bukkit.craftbukkit.v1_12_R1.CraftServer
import org.bukkit.craftbukkit.v1_12_R1.util.Versioning
import java.io.File
import java.lang.management.ManagementFactory
import java.util.jar.Attributes.Name.MAIN_CLASS
import java.util.jar.JarFile
import java.util.logging.Level
import kotlin.system.exitProcess

/**
 * @author cookiedragon234 12/Apr/2020
 */
fun main(args: Array<String>) {
	try {
		if (args.isEmpty()) {
			error("Usage java -jar paperbin.jar paperclip.jar")
		}
		
		val file = File(args[0])
		val newArgs = args.drop(1).toTypedArray()
		
		PaperBinInfo // MUST BE INITIALIZED BEFORE CLASS HOOK
		NativeAccessor.registerClassLoadHook(PaperBinTransformer)
		
		val cl = ClassLoader.getSystemClassLoader()
		NativeAccessor.appendToClassloader(file.absolutePath, false)
		
		val mainClass = run {
			JarFile(file).manifest.mainAttributes.getValue(MAIN_CLASS)
		}
		
		try {
			val clazz = Class.forName(mainClass, true, cl)!!
			val meth = clazz.getDeclaredMethod("main", Array<String>::class.java)!!
			logger.info("Starting [$meth]...")
			meth.invoke(null, newArgs)
		} catch (t: Throwable) {
			throw IllegalStateException("Please provide a valid paperclip jar", t)
		}
	} catch (t: Throwable) {
		handleShutdown(t)
	}
}

fun handleShutdown(t: Throwable): Nothing {
	PaperBinInfo.crashed = true
	
	val version = try {
		PaperBinInfo.version
	} catch (t: Throwable) {
		null
	}
	
	val bukkitVersion = try {
		Versioning.getBukkitVersion()
	} catch (t: Throwable) {
		null
	}
	
	val bukkitServerVersion = try {
		CraftServer::class.java.getPackage().implementationVersion
	} catch (t: Throwable) {
		null
	}
	
	val mcVersion = try {
		MinecraftServer.getServer().version
	} catch (t: Throwable) {
		null
	}
	
	logger.warning(
		"""
			|WARNING: A fatal exception occured while initialising PaperBin $version
			|
			|System Info:
			|   Title: ${System.getProperty("java.vm.name")}
			|   Vendor: ${System.getProperty("java.vm.vendor")}
			|   Version: ${System.getProperty("java.version")}
			|   RT Version: ${System.getProperty("java.runtime.version")}
			|   OS: ${System.getProperty("os.name")}
			|   OS_V: ${System.getProperty("os.version")}
			|   Arch: ${System.getProperty("os.arch")}
			|   JHome: ${System.getProperty("java.home")}
			|   Bukkit Version: $bukkitVersion
			|   Bukkit Server Version: $bukkitServerVersion
			|   Minecraft Version: $mcVersion
		""".trimMargin()
	)
	
	logger.log(Level.SEVERE, "", t)
	exitProcess(0)
}
