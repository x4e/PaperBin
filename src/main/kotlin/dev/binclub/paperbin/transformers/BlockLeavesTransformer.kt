package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.internalName
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 23/Apr/2020
 */
object BlockLeavesTransformer: PaperFeatureTransformer {
	override fun transformClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			if (method.name == "b" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/IBlockData;Ljava/util/Random;)V") {
				for (insn in method.instructions) {
					if (insn is FieldInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "isClientSide") {
						val list = InsnList().apply {
							val jumpOver = LabelNode()
							add(MethodInsnNode(INVOKESTATIC, BlockLeavesTransformer::class.internalName, "shouldDecayLeaf", "()Z", false))
							add(JumpInsnNode(IFNE, jumpOver))
							add(InsnNode(RETURN))
							add(jumpOver)
						}
						
						method.instructions.insertBefore(insn, list)
						return
					}
				}
			}
		}
		error("Couldnt find target")
	}
	
	@JvmStatic
	fun shouldDecayLeaf(): Boolean {
		return false
	}
}
