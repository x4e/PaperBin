package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.internalName
import net.minecraft.server.v1_12_R1.PersistentVillage
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 24/Apr/2020
 */
object PersistentVillageTransformer: PaperFeatureTransformer {
	override fun transformClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			if (method.name == "tick" && method.desc == "()V") {
				for (insn in method.instructions) {
					if (insn.opcode == PUTFIELD) {
						val list = InsnList().apply {
							add(VarInsnNode(ALOAD, 0))
							add(MethodInsnNode(INVOKESTATIC,
								PersistentVillageTransformer::class.internalName,
								"shouldTickVillage",
								"(Ljava/lang/Object;)Z",
								false
							))
							val jumpOver = LabelNode()
							add(JumpInsnNode(IFNE, jumpOver))
							add(InsnNode(RETURN))
							add(jumpOver)
						}
						method.instructions.insert(insn, list)
						return
					}
				}
			}
		}
		error("Couldnt find target")
	}
	
	@JvmStatic
	fun shouldTickVillage(village: Any): Boolean {
		village as PersistentVillage
		
		return PaperBinInfo.ticks % 10 == 0 // run every 10 ticks
	}
}
