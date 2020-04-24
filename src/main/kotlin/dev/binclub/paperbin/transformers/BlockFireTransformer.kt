package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.internalName
import net.minecraft.server.v1_12_R1.BlockFire
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 24/Apr/2020
 */
object BlockFireTransformer: PaperFeatureTransformer {
	override fun transformClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			if (method.name == "b" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/IBlockData;Ljava/util/Random;)V") {
				val list = InsnList().apply {
					add(VarInsnNode(ALOAD, 0))
					add(MethodInsnNode(
						INVOKESTATIC,
						BlockFireTransformer::class.internalName,
						"shouldDoFireTick",
						"(Ljava/lang/Object;)V",
						false
					))
					val jumpOver = LabelNode()
					add(JumpInsnNode(IFNE, jumpOver))
					add(InsnNode(RETURN))
					add(jumpOver)
				}
				
				return
			}
		}
		error("Couldnt find target")
	}
	
	@JvmStatic
	fun shouldDoFireTick(block: Any): Boolean {
		block as BlockFire
		
		return PaperBinInfo.ticks % 10 == 0
	}
}
