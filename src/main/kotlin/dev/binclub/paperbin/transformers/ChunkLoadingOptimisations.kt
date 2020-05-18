package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.add
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 12/May/2020
 */
object ChunkLoadingOptimisations: PaperFeature {
	override fun postStartup() {
	}
	
	override fun registerTransformers() {
		if (!PaperBinConfig.chunkLoadOptimisations) return
		
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
					count += 1
				}
			}
			if (count != 2) {
				error("Couldnt find target")
			}
		}
	}
}
