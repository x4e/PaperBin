package dev.binclub.paperbin

import net.minecraft.server.v1_12_R1.MinecraftServer
import org.apache.openjpa.enhance.InstrumentationFactory
import org.bukkit.craftbukkit.v1_12_R1.CraftServer
import org.bukkit.craftbukkit.v1_12_R1.util.Versioning
import java.io.File
import java.lang.management.ManagementFactory
import java.net.URL
import java.net.URLClassLoader
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
		
		if (!ManagementFactory.getRuntimeMXBean().inputArguments.any {
				it.contains("noverify", true) || it.contains("Xverify", true)
			}) {
			//error("Disable the verifier")
		}
		
		val file = File(args[0])
		val newArgs = args.drop(1).toTypedArray()
		
		if (InstrumentationFactory.getInstrumentation(PaperBinInfo.logger)?.addTransformer(PaperBinTransformer) == null) {
			error(
				"""
				|Could not fetch an instrumentation instance.
				|   Please make sure you have a valid JAVA_HOME specified.
				|   Try adding `-XX:+StartAttachListener` to the jvm launch options
				|   If the error persists try using Open JDK 1.8.0_252.
				|   If the error still persists open an issue at https://github.com/cookiedragon234/PaperBin/issues
			""".trimMargin()
			)
		}
		
		val sysCl = ClassLoader.getSystemClassLoader() as URLClassLoader
		
		URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java).let {
			it.isAccessible = true
			it.invoke(sysCl, file.toURI().toURL())
		}
		
		val mainClass = run {
			JarFile(file).manifest.mainAttributes.getValue(MAIN_CLASS)
		}
		
		PaperBinInfo.logger.info("Starting [$mainClass]...")
		try {
			Class.forName(mainClass, true, sysCl)!!.getDeclaredMethod("main", Array<String>::class.java)!!
				.invoke(null, newArgs)
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
	}
	
	val bukkitServerVersion = try {
		CraftServer::class.java.getPackage().implementationVersion
	} catch (t: Throwable) {
	}
	
	val mcVersion = try {
		MinecraftServer.getServer().version
	} catch (t: Throwable) {
	}
	
	PaperBinInfo.logger.warning(
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
	
	PaperBinInfo.logger.log(Level.SEVERE, "", t)
	exitProcess(0)
}
