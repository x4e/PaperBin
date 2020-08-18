package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.insnBuilder
import dev.binclub.paperbin.utils.internalName
import net.minecraft.server.v1_12_R1.*
import org.bukkit.Bukkit
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 11/May/2020
 */
object VillageRateLimiter: PaperBinFeature {
	const val rateLimit = 40 // run every 40 ticks
	
	@JvmStatic
	fun shouldTickVillage(village: Any): Boolean {
		village as PersistentVillage
		
		if (!PaperBinConfig.villageRateLimit) return true
		
		return PaperBinInfo.ticks % rateLimit == 0
	}
	
	@JvmStatic
	fun golemSpawnRate(original: Int): Int {
		if (!PaperBinConfig.villageRateLimit) return original
		// Compensate both for our nerfed village tick rate, but also for global tps
		val tpsRate = Bukkit.getTPS()[0] / 20
		return (original / rateLimit * tpsRate).toInt()
	}
	
	override fun registerTransformers() {
		if (!PaperBinConfig.villageRateLimit) return
		
		register("net.minecraft.server.v1_12_R1.PersistentVillage") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "tick" && method.desc == "()V") {
					for (insn in method.instructions) {
						if (insn.opcode == PUTFIELD) {
							val list = InsnList().apply {
								add(VarInsnNode(ALOAD, 0))
								add(
									MethodInsnNode(
									INVOKESTATIC,
									VillageRateLimiter::class.internalName,
									"shouldTickVillage",
									"(Ljava/lang/Object;)Z",
									false
								)
								)
								val jumpOver = LabelNode()
								add(JumpInsnNode(IFNE, jumpOver))
								add(InsnNode(RETURN))
								add(jumpOver)
							}
							method.instructions.insert(insn, list)
							return@register
						}
					}
				}
			}
			error("Couldnt find target")
		}
		// Since this will make golems less likely to spawn, lets increase their spawn rate to compensate
		register("net.minecraft.server.v1_12_R1.Village") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(I)V") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "java/util/Random" && insn.name == "nextInt" && insn.desc == "(I)I") {
							val insert = insnBuilder {
								+MethodInsnNode(
									INVOKESTATIC,
									VillageRateLimiter::class.internalName,
									"golemSpawnRate",
									"(I)I",
									false
								)
							}
							method.instructions.insertBefore(insn, insert)
							return@register
						}
					}
				}
			}
			error("Couldnt find target")
		}
	}
}
