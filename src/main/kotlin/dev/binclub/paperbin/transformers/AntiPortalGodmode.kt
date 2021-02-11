package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.insnBuilder
import net.minecraft.server.v1_12_R1.EntityPlayer
import net.minecraft.server.v1_12_R1.PacketPlayInFlying
import net.minecraft.server.v1_12_R1.Vec3D
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * Attempts to patch the "Portal Godmode" exploit where the player indefinately holds onto their dimension change
 * invulnerability
 *
 * @author cookiedragon234 19/May/2020
 */
object AntiPortalGodmode: PaperBinFeature {
	@JvmStatic
	fun checkPositionPacket(teleportPos: Any, player: Any, packet: Any) {
		if (!PaperBinConfig.antiPortalGodmode) return
		teleportPos as Vec3D
		player as EntityPlayer
		packet as PacketPlayInFlying
		
		if (
			packet.a(Double.NaN) == teleportPos.x
			&&
			packet.b(Double.NaN) == teleportPos.y
			&&
			packet.c(Double.NaN) == teleportPos.z
		) {
			player.M() // no longer invulnerable
		}
	}
	
	override fun registerTransformers() {
		if (!PaperBinConfig.antiPortalGodmode) return
		
		register("net.minecraft.server.v1_12_R1.PlayerConnection") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/PacketPlayInFlying;)V") {
					for (insn in method.instructions) {
						if (insn is JumpInsnNode && insn.opcode == IFNULL) {
							val list = insnBuilder {
								+VarInsnNode(ALOAD, 0)
								+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PlayerConnection", "teleportPos", "Lnet/minecraft/server/v1_12_R1/Vec3D;")
								+VarInsnNode(ALOAD, 0)
								+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PlayerConnection", "player", "Lnet/minecraft/server/v1_12_R1/EntityPlayer;")
								+VarInsnNode(ALOAD, 1)
								+MethodInsnNode(INVOKESTATIC, "dev/binclub/paperbin/transformers/AntiPortalGodmode", "checkPositionPacket", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false)
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
