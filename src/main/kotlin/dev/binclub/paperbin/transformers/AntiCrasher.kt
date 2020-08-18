package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.insnBuilder
import net.minecraft.server.v1_12_R1.ItemStack
import net.minecraft.server.v1_12_R1.Items
import net.minecraft.server.v1_12_R1.PacketPlayInWindowClick
import net.minecraft.server.v1_12_R1.PlayerConnection
import org.objectweb.asm.tree.*

/**
 * This wont fix every exploit, but it prevents players picking up books that are abnormally large
 *
 * @author cookiedragon234 12/May/2020
 */
object AntiCrasher: PaperBinFeature {
	@JvmStatic
	fun checkPacket(connection: PlayerConnection, packet: PacketPlayInWindowClick): ItemStack? {
		val stack = packet.e()
		if (stack.item == Items.WRITABLE_BOOK && stack.tag?.hasKey("pages") == true) {
			return stack
		}
		return null
	}
	
	override fun registerTransformers() {
		if (!PaperBinConfig.antiCrasher) return
		
		register("net.minecraft.server.v1_12_R1.PlayerConnection") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/PacketPlayInWindowClick;)V") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.name == "isSpectator" && insn.desc == "()Z") {
							val list = insnBuilder {
								aload(0) // this
								aload(1) // packet
								invokestatic(
									"dev/binclub/paperbin/transformers/AntiCrasher",
									"checkPacket",
									"(Lnet/minecraft/server/v1_12_R1/PlayerConnection;Lnet/minecraft/server/v1_12_R1/PacketPlayInWindowClick;)Lnet/minecraft/server/v1_12_R1/ItemStack;"
								)
								val out = LabelNode()
								dup()
								ifnull(out)
								aload(0) // this
								swap()
								invokevirtual(
									"net/minecraft/server/v1_12_R1/PlayerConnection",
									"validateBook",
									"(Lnet/minecraft/server/v1_12_R1/ItemStack;)Z"
								)
								pop()
								aconst_null()
								+out
								pop()
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
