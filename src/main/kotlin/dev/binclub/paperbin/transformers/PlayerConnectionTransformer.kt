package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperFeatureTransformer
import dev.binclub.paperbin.utils.internalName
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * @author cookiedragon234 24/Apr/2020
 */
object PlayerConnectionTransformer: PaperFeatureTransformer("net.minecraft.server.v1_12_R1.PlayerConnection") {
	override fun transformClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			if (method.name == "handleCommand" && method.desc == "(Ljava/lang/String;)V") {
				val list = InsnList().apply {
					add(VarInsnNode(ALOAD, 1))
					add(MethodInsnNode(INVOKESTATIC, PlayerConnectionTransformer::class.internalName, "onCommand", "(Ljava/lang/String;)V", false))
				}
				method.instructions.insert(list)
				return
			}
		}
		error("Couldnt find target")
	}
	
	@JvmStatic
	fun onCommand(command: String) {
	}
}
