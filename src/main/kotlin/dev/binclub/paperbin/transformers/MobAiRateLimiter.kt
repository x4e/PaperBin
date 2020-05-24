package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.notify
import dev.binclub.paperbin.utils.wait
import net.minecraft.server.v1_12_R1.*
import org.objectweb.asm.tree.MethodInsnNode
import kotlin.concurrent.thread

/**
 * @author cookiedragon234 11/May/2020
 */
object MobAiRateLimiter: PaperFeature {
	private val frozen by lazy {
		EntityLiving::class.java.getDeclaredMethod("isFrozen").also {
			it.isAccessible = true
		}
	}
	
	private fun EntityLiving.isFrozen(): Boolean = frozen.invoke(this) as Boolean
	
	var started = false
	val goalSelectionThread: Thread? = if (PaperBinConfig.mobAiMultithreading) thread (name = "Mob Goal Selection", isDaemon = true, start = false) {
		while (true) {
			if (!PaperBinConfig.mobAiMultithreading) {
				try {
					Thread.yield()
				} catch (t: Throwable) { t.printStackTrace() }
				continue
			}
			try {
				MinecraftServer.getServer().worlds.forEach { world ->
					// We need to fetch the entities as a clone of the original underlying arraylist array
					// This is because if an entity is removed from the world on the main thread, it would otherwise
					// cause a concurrentmodificationexception
					
					val entities = (world.entityList as ArrayList).toArray()
					for (entity in entities) {
						entity as Entity
						val entity1 = entity.bJ()
						if (entity1 != null) {
							if (!entity1.dead && entity1.w(entity)) {
								continue
							}
						}
						
						if (!entity.dead && entity !is EntityPlayer) {
							if (entity is EntityInsentient) {
								if (!entity.isFrozen() && entity.cC() && !entity.fromMobSpawner) {
									entity.goalSelector.a()
								}
							}
						}
					}
				}
			} catch (t: Throwable) {
				IllegalStateException("Exception calculating mob goal", t).printStackTrace()
			}
			try {
				// There is no need to calculate goals multiple times per tick. Since we are done, we will wait until
				// we are notified next tick to recalculate the goals.
				val thisThread = Thread.currentThread()
				synchronized(thisThread) {
					thisThread.wait()
				}
			} catch (t: Throwable) { t.printStackTrace() }
		}
	} else null
	
	/**
	 * Called by Tick Counter, here we wake the goal selection thread up so that it can recalculate goals.
	 * If it is still stuck calculating the goals from the last tick, nothing will happen and it can finish that off.
	 */
	fun onTick() {
		if (!PaperBinConfig.mobAiMultithreading) return
		
		if (!started) {
			goalSelectionThread?.start()
			started = true
		} else {
			try {
				goalSelectionThread?.let { thread ->
					synchronized(thread) {
						thread.notify()
					}
				}
			} catch (t: Throwable) {
				t.printStackTrace()
			}
		}
	}
	
	
	override fun registerTransformers() {
		if (!PaperBinConfig.mobAiRateLimit) return
		
		register("net.minecraft.server.v1_12_R1.EntityInsentient") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "doTick" && method.desc == "()V") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/PathfinderGoalSelector" && insn.name == "a" && insn.desc == "()V") {
							method.instructions.remove(insn)
							return@register
						}
					}
				}
			}
			error("Couldnt find target")
		}
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalSelector") { classNode ->
			for (field in classNode.fields) {
				if (field.name == "f" && field.desc == "I") {
					field.value = 30 // Update goals 1/10th of the time
					return@register
				}
			}
			error("Couldnt find target")
		}
	}
}
