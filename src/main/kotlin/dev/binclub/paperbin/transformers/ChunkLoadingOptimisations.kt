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
			var count = 0
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Ljava/util/Random;Lnet/minecraft/server/v1_12_R1/ChunkCoordIntPair;)Z") {
					method.access = ACC_PUBLIC // Remove ACC_SYNCRONISED
					count += 1
				}
			}
			if (count != 1) {
				error("Couldnt find target")
			}
		}
	}
}
