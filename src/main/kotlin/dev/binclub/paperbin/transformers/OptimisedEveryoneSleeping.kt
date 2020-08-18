package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.insnBuilder
import dev.binclub.paperbin.utils.internalName
import net.minecraft.server.v1_12_R1.WorldServer
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * @author cookiedragon234 18/May/2020
 *
 * Fixed by Tigermouthbear on 28/May/2020
 */
object OptimisedEveryoneSleeping: PaperBinFeature {
	override fun registerTransformers() {
		if (!PaperBinConfig.optimisedEveryoneSleeping) return

		register("net.minecraft.server.v1_12_R1.WorldServer") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "everyoneDeeplySleeping" && method.desc == "()Z") {
					method.instructions.clear()
					method.instructions.add(insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/WorldServer", "Q", "Z") // load Q

						+VarInsnNode(ALOAD, 0) // load `this`

						+MethodInsnNode(INVOKESTATIC, OptimisedEveryoneSleeping::class.internalName, "everyoneDeeplySleeping", "(ZLjava/lang/Object;)Z", false)
						+IRETURN.insn()
					})
					return@register
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
	fun everyoneDeeplySleeping(flag: Boolean, worldServer: Any): Boolean {
		worldServer as WorldServer
		
		val players = worldServer.players

		return if (players.size == 0 || worldServer.isClientSide || !flag)
			false
		else
			players.all { player ->
				player.isSpectator || player.isDeeplySleeping || player.fauxSleeping
			}
	}
}
