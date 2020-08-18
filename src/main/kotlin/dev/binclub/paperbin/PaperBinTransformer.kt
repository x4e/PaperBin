package dev.binclub.paperbin

import dev.binclub.paperbin.PaperBinInfo.logger
import org.bukkit.Bukkit
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.logging.Level
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * @author cookiedragon234 12/Apr/2020
 */
object PaperBinTransformer: ClassFileTransformer {
	private val jar: JarOutputStream? =
		if (PaperBinConfig.debug) {
			val file = File("paperbin_patched.jar")
			if (file.exists()) file.delete()
			JarOutputStream(file.outputStream()).also {
				Runtime.getRuntime().addShutdownHook(thread(start = false, isDaemon = false) {
					it.close()
				})
			}
		} else null
	
	val transforming = HashSet<String>()
	
	override fun transform(
		loader: ClassLoader?,
		className: String?,
		classBeingRedefined: Class<*>?,
		protectionDomain: ProtectionDomain?,
		classfileBuffer: ByteArray
	): ByteArray? {
		if (className == null) {
			return classfileBuffer
		}
		
		val className = className.replace('.', '/')
		try {
			transforming += className
			PaperBinInfo.transformers[className]?.let { transformers ->
				logger.log(Level.INFO, "Transforming [$className]...")
				PaperBinInfo.usedTransformers += className
				val classNode = ClassNode()
				ClassReader(classfileBuffer).accept(classNode, 0)
				
				transformers.forEach {
					try {
						it.invoke(classNode)
					} catch (t: Throwable) {
						logger.log(Level.SEVERE, "Error transforming [$className] with transformer [$it]", t)
						PaperBinInfo.crashed = true
						exitProcess(-1)
					}
				}
				
				val writer = NoLoadClassWriter(ClassWriter.COMPUTE_FRAMES)
				try {
					classNode.accept(writer)
					
					return writer.toByteArray().also {
						if (jar != null) {
							jar.putNextEntry(JarEntry("$className.class"))
							jar.write(it)
							jar.closeEntry()
						}
					}
				} catch (t: Throwable) {
					logger.log(Level.SEVERE, "Error transforming [$className]", t)
					PaperBinInfo.crashed = true
					
					try {
						classNode.methods?.forEach {
							// hacky
							it.maxStack += 10
							it.maxLocals += 5
						}
						val writer = NoLoadClassWriter(ClassWriter.COMPUTE_MAXS)
						classNode.accept(writer)
						classNode.accept(CheckClassAdapter(null, true))
					} catch (t: Throwable) {
						logger.log(Level.SEVERE, "", t)
					}
					
					try {
						if (jar != null) {
							val writer = NoLoadClassWriter(0)
							
							classNode.accept(writer)
							
							writer.toByteArray().also {
								jar.putNextEntry(JarEntry("$className.class"))
								jar.write(it)
								jar.closeEntry()
							}
						}
					} catch (t: Throwable) {}
					
					exitProcess(-1)
				}
			}
			
			return classfileBuffer
		} finally {
			transforming -= className
		}
	}
	
	class NoLoadClassWriter(flags: Int) : ClassWriter(flags) {
		override fun getCommonSuperClass(type1: String, type2: String): String {
			if (type1 == type2) return type1
			if (type1 in transforming || type2 in transforming)	return "java/lang/Object"
			
			return super.getCommonSuperClass(type1, type2)
		}
	}
}
