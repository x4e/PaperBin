package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.add
import dev.binclub.paperbin.utils.printlnAsm
import net.minecraft.server.v1_12_R1.ChunkRegionLoader
import net.minecraft.server.v1_12_R1.PlayerChunk
import net.minecraft.server.v1_12_R1.StructureGenerator
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.util.concurrent.Semaphore

/**
 * @author cookiedragon234 12/May/2020
 */
object ChunkLoadingOptimisations: PaperFeature {
	override fun registerTransformers() {
		if (!PaperBinConfig.chunkLoadOptimisations) return
		
		register("net.minecraft.server.v1_12_R1.PlayerChunk") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "b" && method.desc == "()Z") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/PacketPlayOutMapChunk" && insn.name == "<init>") {
							val list = InsnList().apply {
								val out = LabelNode()
								add(VarInsnNode(ALOAD, 0))
								add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PlayerChunk", "c", "Ljava/util/List;"))
								add(MethodInsnNode(INVOKEINTERFACE, "java/util/List", "isEmpty", "()Z", true))
								add(JumpInsnNode(IFNE, out))
								add(ICONST_1)
								add(IRETURN)
							}
							method.instructions.insertBefore(insn, list)
							return@register
						}
					}
				}
			}
			error("Couldnt find target")
		}
		
		register("net.minecraft.server.v1_12_R1.ChunkRegionLoader") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "processSaveQueueEntry" && method.desc == "(Z)Z") {
					method.access = ACC_PRIVATE // remove synchronized
					return@register
				}
			}
			error("Couldnt find target")
		}
		
		register("net.minecraft.server.v1_12_R1.StructureGenerator") { classNode ->
			val semaphore = FieldNode(
				ACC_PRIVATE,
				"paperbin\$barrier",
				"Ljava/util/concurrent/Semaphore;",
				null,
				null
			)
			classNode.fields.add(semaphore)
			
			var count = 0
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/StructureGenerator;Lnet/minecraft/server/v1_12_R1/BlockPosition;IIIZIZ)Lnet/minecraft/server/v1_12_R1/BlockPosition;") {
				
				}
				
				if (method.name == "<init>") {
					for (insn in method.instructions) {
						if (insn.opcode == RETURN) {
							val list = InsnList().apply {
								add(VarInsnNode(ALOAD, 0))
								add(TypeInsnNode(NEW, "java/util/concurrent/Semaphore"))
								add(DUP)
								add(ICONST_1)
								add(MethodInsnNode(INVOKEVIRTUAL, "java/util/concurrent/Semaphore", "<init>", "(I)V", false))
								add(FieldInsnNode(PUTFIELD, classNode.name, semaphore.name, semaphore.desc))
							}
							method.instructions.insertBefore(insn, list)
							count += 1
						}
					}
				}
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Ljava/util/Random;Lnet/minecraft/server/v1_12_R1/ChunkCoordIntPair;)Z") {
					method.access = ACC_PUBLIC // Remove ACC_SYNCRONISED
					val sL = LabelNode()
					val start = InsnList().apply {
						val end = LabelNode()
						add(sL)
						add(VarInsnNode(ALOAD, 0))
						add(FieldInsnNode(GETFIELD, classNode.name, semaphore.name, semaphore.desc))
						add(MethodInsnNode(INVOKEVIRTUAL, "java/util/concurrent/Semaphore", "acquire", "()V", false))
						add(MethodInsnNode(INVOKESTATIC, "org/bukkit/Bukkit", "isPrimaryThread", "()Z", false))
						add(JumpInsnNode(IFNE, end))
						add(printlnAsm("async chunk loading"))
						add(end)
					}
					method.instructions.insert(start)
					
					for (insn in method.instructions) {
						if (insn.opcode == IRETURN) {
							val release = LabelNode()
							val handler = LabelNode()
							val endL = LabelNode()
							val end = InsnList().apply {
								add(endL)
								add(JumpInsnNode(GOTO, release))
								add(handler)
								add(VarInsnNode(ALOAD, 0))
								add(FieldInsnNode(GETFIELD, classNode.name, semaphore.name, semaphore.desc))
								add(MethodInsnNode(INVOKEVIRTUAL, "java/util/concurrent/Semaphore", "release", "()V", false))
								add(ATHROW)
								add(release)
								add(VarInsnNode(ALOAD, 0))
								add(FieldInsnNode(GETFIELD, classNode.name, semaphore.name, semaphore.desc))
								add(MethodInsnNode(INVOKEVIRTUAL, "java/util/concurrent/Semaphore", "release", "()V", false))
							}
							method.instructions.insertBefore(insn, end)
							method.tryCatchBlocks = method.tryCatchBlocks ?: arrayListOf()
							method.tryCatchBlocks.add(TryCatchBlockNode(sL, endL, handler, null))
							count += 1
							break
						}
					}
				}
			}
			if (count != 2) {
				error("Couldnt find target")
			}
		}
	}
}
