package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.add
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * This wont fix every exploit, but it prevents players picking up books that are abnormally large
 *
 * @author cookiedragon234 12/May/2020
 */
object AntiCrasher: PaperFeature {
	override fun registerTransformers() {
		if (!PaperBinConfig.antiCrasher) return
		
		register("net.minecraft.server.v1_12_R1.PlayerConnection") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/PacketPlayInWindowClick;)V") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.name == "isSpectator" && insn.desc == "()Z") {
							
							val endJump1 = LabelNode()
							val endJump2 = LabelNode()
							val list = InsnList().apply {
								add(VarInsnNode(ALOAD, 1))// PacketPlayInWindowClick
								add(MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/server/v1_12_R1/PacketPlayInWindowClick", "e", "()Lnet/minecraft/server/v1_12_R1/ItemStack;", false))
								add(DUP)
								
								add(MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/server/v1_12_R1/ItemStack", "getItem", "()Lnet/minecraft/server/v1_12_R1/Item;", false))
								add(FieldInsnNode(GETSTATIC, "net/minecraft/server/v1_12_R1/Items", "WRITABLE_BOOK", "Lnet/minecraft/server/v1_12_R1/Item;"))
								add(JumpInsnNode(IF_ACMPNE, endJump1)) // If item is book
								
								add(VarInsnNode(ALOAD, 0))
								add(SWAP)
								add(MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/server/v1_12_R1/PlayerConnection", "PaperBinInfo.logger.infook", "(Lnet/minecraft/server/v1_12_R1/ItemStack;)Z", false))
								add(JumpInsnNode(IFNE, endJump2))
								
								add(RETURN) // Dont process, player will be disconnected next tick
								
								add(endJump1)
								add(POP)
								add(endJump2)
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
