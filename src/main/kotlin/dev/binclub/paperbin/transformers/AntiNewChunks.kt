package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.add
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode

/**
 * Forces all packets to be "full chunks"
 *
 * This causes tons of client side errors, so its disabled by default
 *
 * @author cookiedragon234 15/May/2020
 */
object AntiNewChunks: PaperBinFeature {
	override fun registerTransformers() {
		if (!PaperBinConfig.antiNewChunks) return
		
		register("net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "b" && method.desc == "(Lnet/minecraft/server/v1_12_R1/PacketDataSerializer;)V") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/PacketDataSerializer" && insn.name == "writeBoolean" && insn.desc == "(Z)Lio/netty/buffer/ByteBuf;") {
							val list = InsnList().apply {
								add(POP)
								add(ICONST_1)
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
}
