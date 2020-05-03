package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperFeatureTransformer
import dev.binclub.paperbin.utils.internalName
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 25/Apr/2020
 */
object BlockStationaryTransformer: PaperFeatureTransformer("net.minecraft.server.v1_12_R1.BlockStationary") {
	override fun transformClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			if (method.name == "b" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/IBlockData;Ljava/util/Random;)V") {
				val list = InsnList().apply {
					add(VarInsnNode(ALOAD, 0))
					add(MethodInsnNode(
						Opcodes.INVOKESTATIC,
						BlockTransformer::class.internalName,
						"shouldTickBlock",
						"(Ljava/lang/Object;)Z",
						false
					))
					val jumpOver = LabelNode()
					add(JumpInsnNode(IFNE, jumpOver))
					add(InsnNode(RETURN))
					add(jumpOver)
				}
				method.instructions.insert(list)
				
				return
			}
		}
		error("Couldnt find target")
	}
}
