package dev.binclub.paperbin

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

/**
 * @author cookiedragon234 12/Apr/2020
 */
object PaperBinTransformer: ClassFileTransformer {
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
		
		PaperBinInfo.transformers[className.replace('/', '.')]?.let { transformers ->
			println("Transforming [$className]...")
			val classNode = ClassNode()
			ClassReader(classfileBuffer).accept(classNode, 0)
			
			transformers.forEach {
				it.invoke(classNode)
			}
			
			val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
			classNode.accept(writer)
			return writer.toByteArray()
		}
			
		return classfileBuffer
	}
}
