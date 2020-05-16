package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.internalName
import net.minecraft.server.v1_12_R1.*
import org.bukkit.Bukkit
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 11/May/2020
 */
object VillageRateLimiter: PaperFeature {
	@JvmStatic
	fun shouldTickVillage(village: Any): Boolean {
		village as PersistentVillage
		
		if (!PaperBinConfig.villageRateLimit) return true
		
		return PaperBinInfo.ticks % 40 == 0 // run every 10 ticks
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
	}
}
