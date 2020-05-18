package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.add
import dev.binclub.paperbin.utils.insnBuilder
import net.minecraft.server.v1_12_R1.EntityHuman
import net.minecraft.server.v1_12_R1.WorldServer
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Opcodes.IRETURN
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode

/**
 * @author cookiedragon234 18/May/2020
 */
object OptimisedEveryoneSleeping: PaperFeature {
	override fun registerTransformers() {
		register("net.minecraft.server.v1_12_R1.WorldServer") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "everyoneDeeplySleeping" && method.desc == "()Z") {
					for (insn in method.instructions) {
						if (insn is FieldInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/WorldServer" && insn.name == "players" && insn.desc == "Ljava/util/List;") {
							val list = insnBuilder {
								+MethodInsnNode(INVOKESTATIC, "dev/binclub/paperbin/transformers/WorldServerOptimisations", "everyoneDeeplySleeping", "(Ljava/lang/Object;)Z", false)
								+IRETURN.insn()
							}
							method.instructions.insertBefore(insn, list)
							return@register
						}
					}
				}
			}
			error("Couldnt find target")
		}
	}
	
	/**
	 * Optimised algorithm to check if all players in the world are sleeping, used to fast forward to next day
	 *
	 * This also fixes MC-47080
	 */
	@JvmStatic
	fun everyoneDeeplySleeping(worldServer: Any): Boolean {
		worldServer as WorldServer
		
		for (player in worldServer.players) {
			// All players must either be fake, deeply sleeping or a spectator
			if (!player.isDeeplySleeping && !player.fauxSleeping && !player.isSpectator) {
				return false
			}
		}
		return true
	}
}
