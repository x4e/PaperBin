package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinInfo
import net.minecraft.server.v1_12_R1.EntityVillager
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 28/Apr/2020
 */
object EntityVillagerTransformer: PaperFeatureTransformer {
	override fun transformClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			if (method.name == "M" && method.desc == "()V") {
				classNode.methods.add(MethodNode(
					method.access,
					method.name,
					method.desc,
					method.signature,
					null
				).apply {
					instructions.apply {
						add(VarInsnNode(ALOAD, 0))
						add(InsnNode(ICONST_1))
						add(MethodInsnNode(INVOKEVIRTUAL, classNode.name, method.name, "(Z)V", false))
						add(InsnNode(RETURN))
					}
				})
				method.desc = "(Z)V"
				for (insn in method.instructions) {
					if (insn.opcode == INVOKESPECIAL) {
						insn as MethodInsnNode
						
						if (insn.owner == "net/minecraft/server/v1_12_R1/EntityAgeable" && insn.name == "M" && insn.desc == "()V") {
							val list = InsnList().apply {
								val jumpOver = LabelNode()
								add(VarInsnNode(ILOAD, 5))
								add(JumpInsnNode(IFEQ, jumpOver))
								add(InsnNode(RETURN))
								add(jumpOver)
							}
							method.instructions.insertBefore(insn, list)
							
							method.instructions.insert(InsnList().apply {
								add(VarInsnNode(ILOAD, 1))
								add(VarInsnNode(ISTORE, 5))
							})
							return
						}
					}
				}
			}
		}
		error("Target not found")
	}
	
	@JvmStatic
	fun shouldUpdateVillager(villager: Any): Boolean {
		villager as EntityVillager
		
		return PaperBinInfo.ticks % 15 == 0
	}
	
	val M by lazy {
		EntityVillager::class.java.getDeclaredMethod("M", java.lang.Boolean.TYPE).also {
			it.isAccessible = true
		}
	}
	
	fun handleInsentientVillagerUpdate(update: Boolean, villager: Any) {
		if (!update) {
			M(villager, false)
		}
	}
}
