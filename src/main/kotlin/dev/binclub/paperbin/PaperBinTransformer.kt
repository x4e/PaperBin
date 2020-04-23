package dev.binclub.paperbin

import dev.binclub.paperbin.transformers.BlockLeavesTransformer
import dev.binclub.paperbin.transformers.EntityInsentientTransformer
import dev.binclub.paperbin.transformers.MinecraftServerTransformer
import dev.binclub.paperbin.transformers.PaperFeatureTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

/**
 * @author cookiedragon234 12/Apr/2020
 */
object PaperBinTransformer: ClassFileTransformer {
	private val transformers: MutableMap<String, PaperFeatureTransformer> = hashMapOf()
	
	init {
		transformers["net.minecraft.server.v1_12_R1.EntityInsentient"] = EntityInsentientTransformer
		transformers["net.minecraft.server.v1_12_R1.BlockLeaves"] = BlockLeavesTransformer
		transformers["net.minecraft.server.v1_12_R1.MinecraftServer"] = MinecraftServerTransformer
	}
	
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
		
		transformers[className.replace('/', '.')]?.let {
			println("Transforming [$className]...")
			val classNode = ClassNode()
			ClassReader(classfileBuffer).accept(classNode, 0)
			
			it.transformClass(classNode)
			
			val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
			classNode.accept(writer)
			return writer.toByteArray()
		}
			
		return classfileBuffer
	}
}
