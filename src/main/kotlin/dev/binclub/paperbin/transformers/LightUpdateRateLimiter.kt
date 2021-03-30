@file:Suppress("NOTHING_TO_INLINE")

package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.utils.forEach
import dev.binclub.paperbin.utils.insnBuilder
import net.minecraft.server.v1_12_R1.BlockPosition
import net.minecraft.server.v1_12_R1.Chunk
import net.minecraft.server.v1_12_R1.MCUtil
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 27/Aug/2020
 */
object LightUpdateRateLimiter: PaperBinFeature {
	override fun registerTransformers() {
		if (!PaperBinConfig.lightUpdateRateLimit) return
		
		logger.info("Make sure to enable queue-light-updates in paper config")
		
		register("net/minecraft/server/v1_12_R1/PaperLightingQueue\$LightingQueue") { cn ->
			// THIS CLASS SHOULD NOT BE USED!!
			cn.fields?.clear()
			cn.methods = arrayListOf()
			cn.methods.add(MethodNode(
				ACC_STATIC,
				"<clinit>",
				"()V",
				null,
				null
			).apply {
				instructions = insnBuilder {
					new(UnsupportedOperationException::class)
					dup()
					ldc("PaperBin incompatibility")
					invokespecial(UnsupportedOperationException::class, "<init>", "(Ljava/lang/String;)V")
					athrow()
				}
			})
		}
		
		register("dev/binclub/paperbin/transformers/CustomLightingQueue") { cn ->
			cn.methods.forEach { mn ->
				if (mn.name == "PROCESSNEIGHBOURQUEUE") {
					mn.instructions = insnBuilder {
						aload(1)
						getfield("net/minecraft/server/v1_12_R1/Chunk", "lightingQueue", "Ldev/binclub/paperbin/transformers/CustomLightingQueue;")
						ldc(0L)
						ldc(0L)
						invokevirtual("dev/binclub/paperbin/transformers/CustomLightingQueue", "processQueue", "(JJ)Z")
						_return()
					}
				}
			}
		}
		
		register("net.minecraft.server.v1_12_R1.Chunk") { cn ->
			var count = 0
			cn.fields.forEach { fn ->
				if (fn.name == "lightingQueue") {
					fn.access = ACC_PUBLIC
					count += 1
				}
			}
			cn.methods.forEach { mn ->
				if (mn.name == "runOrQueueLightUpdate" && mn.desc == "(Ljava/lang/Runnable;)V") {
					mn.desc = "(Ljava/lang/Runnable;Lnet/minecraft/server/v1_12_R1/BlockPosition;)V"
					count += 1
					mn.instructions.forEach { insn ->
						if (insn is MethodInsnNode && insn.opcode == INVOKEVIRTUAL) {
							if (insn.name == "add" && insn.desc == "(Ljava/lang/Object;)Z") {
								insn.desc = "(Lnet/minecraft/server/v1_12_R1/BlockPosition;Ljava/lang/Runnable;)Z"
								mn.instructions.insertBefore(insn, insnBuilder {
									aload(2)
									swap()
								})
								count += 1
							}
						}
					}
				} else if (mn.name == "a" && mn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/IBlockData;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
					mn.instructions.forEach { insn ->
						if (insn is MethodInsnNode && insn.opcode == INVOKEVIRTUAL) {
							if (insn.name == "runOrQueueLightUpdate" && insn.desc == "(Ljava/lang/Runnable;)V") {
								insn.desc = "(Ljava/lang/Runnable;Lnet/minecraft/server/v1_12_R1/BlockPosition;)V"
								mn.instructions.insertBefore(insn, insnBuilder {
									aload(1)
								})
								count += 1
							}
						}
					}
				}
			}
			if (count != 4) {
				error("Couldn't find targets ($count)")
			}
		}
		
		register("net/minecraft/server/v1_12_R1/World") { cn ->
			var count = 0
			cn.methods.forEach { mn ->
				if (mn.name == "setTypeAndData" && mn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/IBlockData;I)Z") {
					mn.instructions.forEach { insn ->
						if (insn is MethodInsnNode && insn.opcode == INVOKEVIRTUAL) {
							if (insn.name == "runOrQueueLightUpdate" && insn.desc == "(Ljava/lang/Runnable;)V") {
								insn.desc = "(Ljava/lang/Runnable;Lnet/minecraft/server/v1_12_R1/BlockPosition;)V"
								mn.instructions.insertBefore(insn, insnBuilder {
									aload(1)
								})
								count += 1
							}
						}
					}
				}
			}
			if (count != 1) {
				error("Couldn't find targets ($count)")
			}
		}
		
		register("net/minecraft/server/v1_12_R1/Chunk", this::map)
		register("net/minecraft/server/v1_12_R1/PaperLightingQueue", this::map)
		register("net/minecraft/server/v1_12_R1/ChunkProviderServer", this::map)
	}
	
	fun map(cn: ClassNode) {
		cn.fields.forEach { fn ->
			if (fn.desc == "Lnet/minecraft/server/v1_12_R1/PaperLightingQueue\$LightingQueue;") {
				fn.desc = "Ldev/binclub/paperbin/transformers/CustomLightingQueue;"
			}
		}
		cn.methods.forEach { mn ->
			mn.instructions?.forEach { insn ->
				if (insn is TypeInsnNode) {
					insn.desc = insn.desc.replace(
						"net/minecraft/server/v1_12_R1/PaperLightingQueue\$LightingQueue",
						"dev/binclub/paperbin/transformers/CustomLightingQueue"
					)
				}
				if (insn is MethodInsnNode) {
					if (insn.opcode == INVOKESTATIC && insn.owner == "net/minecraft/server/v1_12_R1/PaperLightingQueue\$LightingQueue" && insn.name == "access\$000") {
						insn.opcode = INVOKEVIRTUAL
						insn.owner = "dev/binclub/paperbin/transformers/CustomLightingQueue"
						insn.name = "processQueue"
						insn.desc = "(JJ)Z"
					} else {
						insn.owner = insn.owner.replace(
							"net/minecraft/server/v1_12_R1/PaperLightingQueue\$LightingQueue",
							"dev/binclub/paperbin/transformers/CustomLightingQueue"
						)
					}
				}
				if (insn is FieldInsnNode) {
					insn.owner = insn.owner.replace(
						"net/minecraft/server/v1_12_R1/PaperLightingQueue\$LightingQueue",
						"dev/binclub/paperbin/transformers/CustomLightingQueue"
					)
					insn.desc = insn.desc.replace(
						"net/minecraft/server/v1_12_R1/PaperLightingQueue\$LightingQueue",
						"dev/binclub/paperbin/transformers/CustomLightingQueue"
					)
				}
			}
		}
	}
}

class CustomLightingQueue(val chunk: Chunk): HashMap<BlockPosition, Runnable>() {
	private inline fun isOutOfTime(maxTickTime: Long, startTime: Long) =
		startTime > 0L && System.nanoTime() - startTime > maxTickTime
	
	fun add(key: BlockPosition, value: Runnable): Boolean {
		put(key, value)
		return true
	}
	
	private var lastUpdated = 0L
	fun processQueue(startTime: Long, maxTickTime: Long): Boolean {
		val now = System.currentTimeMillis()
		if (now - lastUpdated < PaperBinConfig.lightUpdateRateLimitDelay) {
			return false
		}
		lastUpdated = now
		
		when {
			this.isEmpty() -> {
				return false
			}
			isOutOfTime(maxTickTime, startTime) -> {
				return true
			}
			else -> {
				val ignored = chunk.world.timings.lightingQueueTimer.startTiming()
				var var6: Throwable? = null
				val updates = this.values
				try {
					for (lightUpdate in updates) {
						try {
							lightUpdate.run()
							if (isOutOfTime(maxTickTime, startTime)) {
								return true
							}
						} catch (var18: Throwable) {
							var6 = var18
							throw var18
						} finally {
							if (ignored != null) {
								if (var6 != null) {
									try {
										ignored.close()
									} catch (var17: Throwable) {
										var6.addSuppressed(var17)
									}
								} else {
									ignored.close()
								}
							}
						}
					}
				} catch (e: ConcurrentModificationException) {
					e.printStackTrace()
				}
				return false
			}
		}
	}
	
	fun processUnload() {
		if (chunk.world.paperConfig.queueLightUpdates) {
			processQueue(0L, 0L)
			for (x in (chunk.locX - 1)..(chunk.locX + 1)) {
				for (z in (chunk.locZ - 1)..(chunk.locZ + 1)) {
					if (x != chunk.locX || z != chunk.locZ) {
						val neighbor = MCUtil.getLoadedChunkWithoutMarkingActive(chunk.world, x, z) ?: continue
						PROCESSNEIGHBOURQUEUE(neighbor)
					}
				}
			}
		}
	}
	
	fun PROCESSNEIGHBOURQUEUE(neighbor: Chunk) {
		//neighbor.lightingQueue.processQueue(0L, 0L)
		error("Fatal PaperBin error")
	}
}
