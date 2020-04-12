package dev.binclub.paperbin

import com.sun.tools.attach.VirtualMachine
import java.io.*
import java.lang.instrument.Instrumentation
import java.lang.management.ManagementFactory
import java.net.URLClassLoader
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * @author cookiedragon234 12/Apr/2020
 */
object InstrumentationFactory {
	private var internalInstrumentationInstance: Instrumentation? = null
	
	@get:Synchronized
	val instrumentation: Instrumentation by lazy {
		if (internalInstrumentationInstance == null) {
			if (InstrumentationFactory::class.java.classLoader != ClassLoader.getSystemClassLoader()) {
				error("Wrong classloader")
			}
			loadAgent(getAgentJar())
		}
		internalInstrumentationInstance!!
	}
	
	@JvmStatic
	fun agentmain(agentArgs: String?, inst: Instrumentation?) {
		internalInstrumentationInstance = inst
	}
	
	private fun createAgentJar(): String {
		return File.createTempFile(InstrumentationFactory::class.java.name, ".jar").let { file ->
			ZipOutputStream(FileOutputStream(file)).use {
				it.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
				it.write("""
				|Agent-Class: ${InstrumentationFactory::class.java.name}
				|Can-Redefine-Classes: true
				|Can-Retransform-Classes: true
			""".trimMargin().toByteArray())
			}
			
			file.deleteOnExit()
			file.absolutePath
		}
	}
	
	private fun getAgentJar(): String {
		val agentJarFile = InstrumentationFactory::class.java.protectionDomain.codeSource?.location?.let {
			File(it.file)
		}
		return if (
			agentJarFile == null
			||
			agentJarFile.isDirectory
			||
			!checkValidManifest(agentJarFile, InstrumentationFactory::class.java.name)
		) {
			createAgentJar()
		} else {
			agentJarFile.absolutePath
		}
	}
	
	private fun loadAgent(agentJar: String) {
		val runtime = ManagementFactory.getRuntimeMXBean()
		var pid = runtime.name
		if (pid.contains("@")) pid = pid.substring(0, pid.indexOf("@"))
		val vm = VirtualMachine.attach(pid)
		vm.loadAgent(agentJar)
		vm.detach()
	}
	
	private fun checkValidManifest(agentJarFile: File, agentClassName: String): Boolean {
		try {
			val jar = JarFile(agentJarFile)
			val manifest = jar.manifest ?: return false
			val attributes = manifest.mainAttributes
			val ac = attributes.getValue("Agent-Class")
			if (ac != null && ac == agentClassName) {
				return true
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return false
	}
}
