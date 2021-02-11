package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.add
import net.minecraft.server.v1_12_R1.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 13/May/2020
 */
object AntiChunkBan: PaperBinFeature {
	val dispatchPackets by lazy {
		NetworkManager::class.java.declaredMethods.first { it.name == "dispatchPacket" }.also {
			it.isAccessible = true
		}
	}
	
	@JvmStatic
	fun sendExtraPackets(networkManager: Any, packet: Any, listeners: Any?) {
		packet as Packet<*>
		
		dispatchPackets(
			networkManager,
			packet,
			listeners
		)
		packet.extraPackets?.forEach { extra ->
			dispatchPackets(
				networkManager,
				extra,
				listeners
			)
		}
	}
	
	@JvmStatic
	fun shouldLimit(tileEntity: Any): Boolean {
		return (
			tileEntity is TileEntity
			&&
			tileEntity !is TileEntityChest
			&&
			tileEntity !is TileEntityEnderChest
		)
	}
	
	override fun registerTransformers() {
		if (!PaperBinConfig.antiChunkBan) return
		
		register("net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk") { classNode ->
			var count = 0
			for (method in classNode.methods) {
				if (method.name == "<init>" && method.desc == "(Lnet/minecraft/server/v1_12_R1/Chunk;I)V") {
					for (insn in method.instructions) {
						if (insn is TypeInsnNode && insn.opcode == INSTANCEOF && insn.desc == "net/minecraft/server/v1_12_R1/TileEntitySign") {
							val replacement = MethodInsnNode(INVOKESTATIC, "dev/binclub/paperbin/transformers/AntiChunkBan", "shouldLimit", "(Ljava/lang/Object;)Z", false)
							method.instructions.insertBefore(insn, replacement)
							method.instructions.remove(insn)
							//insn.desc = "net/minecraft/server/v1_12_R1/TileEntity"
							count += 1
						}
						if (insn is MethodInsnNode && insn.name == "getUpdatePacket" && insn.desc == "()Lnet/minecraft/server/v1_12_R1/PacketPlayOutTileEntityData;") {
							val next = insn.next.next
							val afterNext = LabelNode()
							method.instructions.insert(next, afterNext)
							
							val list = InsnList().apply {
								val f = LabelNode()
								val end = LabelNode()
								add(DUP)
								add(JumpInsnNode(IFNULL, f))
								add(JumpInsnNode(GOTO, end))
								add(f)
								add(POP)
								add(POP)
								add(JumpInsnNode(GOTO, afterNext))
								add(end)
							}
							method.instructions.insert(insn, list)
							count += 1
						}
					}
				}
			}
			if (count != 2) {
				error("Couldnt find target $count")
			}
		}
		
		/*register("net.minecraft.server.v1_12_R1.NetworkManager") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "m" && method.desc == "()Z") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/NetworkManager" && insn.name == "dispatchPacket" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/Packet;[Lio/netty/util/concurrent/GenericFutureListener;)V") {
							val replace = InsnList().apply {
								add(MethodInsnNode(INVOKESTATIC, "dev/binclub/paperbin/transformers/AntiChunkBan", "sendExtraPackets", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false))
							}
							
							method.instructions.insertBefore(insn, replace)
							method.instructions.remove(insn)
							
							return@register
						}
					}
				}
			}
			error("Couldnt find target")
		}*/
		
		/*register("net.minecraft.server.v1_12_R1.TileEntity") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "getUpdatePacket" && method.desc == "()Lnet/minecraft/server/v1_12_R1/PacketPlayOutTileEntityData;") {
					val list = InsnList().apply {
						/**
						LINENUMBER 107 L0
						NEW net/minecraft/server/v1_12_R1/PacketPlayOutTileEntityData
						DUP
						ALOAD 0
						GETFIELD net/minecraft/server/v1_12_R1/TileEntitySign.position : Lnet/minecraft/server/v1_12_R1/BlockPosition;
						BIPUSH 9
						ALOAD 0
						INVOKEVIRTUAL net/minecraft/server/v1_12_R1/TileEntitySign.d ()Lnet/minecraft/server/v1_12_R1/NBTTagCompound;
						INVOKESPECIAL net/minecraft/server/v1_12_R1/PacketPlayOutTileEntityData.<init> (Lnet/minecraft/server/v1_12_R1/BlockPosition;ILnet/minecraft/server/v1_12_R1/NBTTagCompound;)V
						ARETURN
						 */
						add(TypeInsnNode(NEW, "net/minecraft/server/v1_12_R1/PacketPlayOutTileEntityData"))
						add(DUP)
						add(VarInsnNode(ALOAD, 0))
						add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/TileEntity", "position", "Lnet/minecraft/server/v1_12_R1/BlockPosition;"))
						add(ldcInt(9))
						add(VarInsnNode(ALOAD, 0))
						add(MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/server/v1_12_R1/TileEntity", "d", "()Lnet/minecraft/server/v1_12_R1/NBTTagCompound", false))
						add(MethodInsnNode(INVOKESPECIAL, "net/minecraft/server/v1_12_R1/PacketPlayOutTileEntityData", "<init>", "(Lnet/minecraft/server/v1_12_R1/BlockPosition;ILnet/minecraft/server/v1_12_R1/NBTTagCompound;)V", false))
						add(ARETURN)
					}
					method.instructions.insert(list)
					return@register
				}
			}
			error("Couldnt find target")
		}*/
	}
}
