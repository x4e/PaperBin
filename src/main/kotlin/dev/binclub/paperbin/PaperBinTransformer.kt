package dev.binclub.paperbin

import org.bukkit.Bukkit
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.logging.Level
import kotlin.system.exitProcess

/**
 * @author cookiedragon234 12/Apr/2020
 */
object PaperBinTransformer: ClassFileTransformer {
	val jar: JarOutputStream? =
		if (PaperBinConfig.debug) {
			JarOutputStream(File("paperbin_patched.jar").outputStream()).also { jar ->
				Runtime.getRuntime().addShutdownHook(Thread {jar.close()})
			}
		} else null
	
	override fun transform(
		loader: ClassLoader?,
		className: String?,
		classBeingRedefined: Class<*>?,
		protectionDomain: ProtectionDomain?,
		classfileBuffer: ByteArray
	): ByteArray? {
		try {
			if (className == null) {
				return classfileBuffer
			}
			
			PaperBinInfo.transformers[className.replace('/', '.')]?.let { transformers ->
				PaperBinInfo.logger.log(Level.INFO, "Transforming [$className]...")
				val classNode = ClassNode()
				ClassReader(classfileBuffer).accept(classNode, 0)
				
				transformers.forEach {
					it.invoke(classNode)
				}
				
				val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)//ClassWriter(ClassWriter.COMPUTE_FRAMES)
				classNode.accept(writer)
				
				return writer.toByteArray().also {
					if (jar != null) {
						jar.putNextEntry(JarEntry("$className.class"))
						jar.write(it)
						jar.closeEntry()
					}
				}
			}
		} catch (t: Throwable) {
			t.printStackTrace()
			exitProcess(-1)
		}
		return classfileBuffer
	}
}
