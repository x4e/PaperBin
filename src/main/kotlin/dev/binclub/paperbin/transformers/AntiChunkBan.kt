package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.add
import dev.binclub.paperbin.utils.ldcInt
import net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk
import net.minecraft.server.v1_12_R1.TileEntity
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 13/May/2020
 */
object AntiChunkBan: PaperFeature {
	override fun registerTransformers() {
		register("net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk") { classNode ->
			var count = 0
			for (method in classNode.methods) {
				if (method.name == "<init>" && method.desc == "(Lnet/minecraft/server/v1_12_R1/Chunk;I)V") {
					for (insn in method.instructions) {
						if (insn is TypeInsnNode && insn.opcode == INSTANCEOF && insn.desc == "net/minecraft/server/v1_12_R1/TileEntitySign") {
							insn.desc = "net/minecraft/server/v1_12_R1/TileEntity"
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
