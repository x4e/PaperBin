package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.internalName
import net.minecraft.server.v1_12_R1.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.IllegalStateException

/**
 * @author cookiedragon234 11/May/2020
 */
object MobAiRateLimiter: PaperFeature {
	@JvmStatic
	fun shouldDoAi(entity: Any): Boolean {
		entity as EntityInsentient
		
		if (!PaperBinInfo.enabled || PaperBinInfo.isTpsHigh()) return true
		
		return when (entity) {
			is EntityBat -> false
			is EntityRabbit -> false
			is EntityPolarBear -> false
			is EntityArmorStand -> false
			is EntitySnowman -> PaperBinInfo.ticks % 25 == 0
			is EntityEndermite -> PaperBinInfo.ticks % 25 == 0
			is EntityParrot -> PaperBinInfo.ticks % 25 == 0
			is EntityPigZombie -> PaperBinInfo.ticks % 25 == 0
			is EntityVillager -> (PaperBinInfo.ticks % 15 == 0).also {
				handleInsentientVillagerUpdate(it, entity)
			}
			is EntityMonster -> PaperBinInfo.ticks % 15 == 0
			else -> PaperBinInfo.ticks % 5 == 0
		} && entity.world.findNearbyPlayer(entity, 40.0) != null // Only calculate if there are nearby players
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
	
	override fun registerTransformers() {
		register("net.minecraft.server.v1_12_R1.EntityInsentient") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "doTick" && method.desc == "()V") {
					for (insn in method.instructions) {
						if (insn is LdcInsnNode) {
							if (insn.cst == "sensing") {
								val load = insn.previous?.previous?.previous ?: error("Null Load")
								
								val list = InsnList().apply {
									val out = LabelNode()
									add(VarInsnNode(ALOAD, 0))
									add(MethodInsnNode(INVOKESTATIC, MobAiRateLimiter::class.internalName, "shouldDoAi", "(Ljava/lang/Object;)Z", false))
									add(JumpInsnNode(IFNE, out))
									add(InsnNode(RETURN))
									add(out)
								}
								
								method.instructions.insertBefore(load, list)
								return@register
							}
						}
					}
				}
			}
			error("Couldnt find target")
		}
		register("net.minecraft.server.v1_12_R1.EntityVillager") { classNode ->
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
								return@register
							}
						}
					}
				}
			}
			error("Target not found")
		}
	}
}
