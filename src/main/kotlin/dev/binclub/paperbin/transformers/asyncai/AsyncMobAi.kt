package dev.binclub.paperbin.transformers.asyncai

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.insnBuilder
import dev.binclub.paperbin.utils.internalName
import dev.binclub.paperbin.utils.notify
import dev.binclub.paperbin.utils.wait
import net.minecraft.server.v1_12_R1.*
import net.minecraft.server.v1_12_R1.BlockPosition.PooledBlockPosition
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import kotlin.concurrent.thread

/**
 * @author cookiedragon234 25/May/2020
 */
object AsyncMobAi: PaperFeature {
	private val frozen by lazy {
		EntityLiving::class.java.getDeclaredMethod("isFrozen").also {
			it.isAccessible = true
		}
	}
	private fun EntityLiving.isFrozen(): Boolean = frozen.invoke(this) as Boolean
	private val setupGoals by lazy {
		PathfinderGoalSelector::class.java.getDeclaredMethod("setupGoals").also {
			it.isAccessible = true
		}
	}
	
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
						try {
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
										//.a()
										setupGoals.invoke(entity.goalSelector)
									}
								}
							}
						} catch (t: Throwable) {
							IllegalStateException("Exception while updating mob AI for $entity", t).printStackTrace()
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
	
	/**
	 * Better version that does not load chunks!
	 */
	@JvmStatic
	fun betterCanEntityStandOn(world: Any, blockPosition: Any): Boolean {
		world as World
		blockPosition as BlockPosition
		return world.getTypeIfLoaded(blockPosition.down())?.b() ?: false
	}
	
	/**
	 * Again, does not load chunks!
	 */
	@JvmStatic
	fun betterIsMaterialInBB(world: Any, axisalignedbb: Any, material: Any): Boolean {
		world as World
		axisalignedbb as AxisAlignedBB
		material as Material
		val i = MathHelper.floor(axisalignedbb.a)
		val j = MathHelper.f(axisalignedbb.d)
		val k = MathHelper.floor(axisalignedbb.b)
		val l = MathHelper.f(axisalignedbb.e)
		val i1 = MathHelper.floor(axisalignedbb.c)
		val j1 = MathHelper.f(axisalignedbb.f)
		val blockposition_pooledblockposition = PooledBlockPosition.s()
		for (k1 in i until j) {
			for (l1 in k until l) {
				for (i2 in i1 until j1) {
					if (world.getTypeIfLoaded(blockposition_pooledblockposition.f(k1, l1, i2))?.material === material) {
						blockposition_pooledblockposition.t()
						return true
					}
				}
			}
		}
		blockposition_pooledblockposition.t()
		return false
	}
	
	fun fixGetType(classNode: ClassNode) {
		for (method in classNode.methods) {
			for (insn in method.instructions) {
				if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
					insn.name = "getTypeIfLoaded"
					println("Removed --------------------------")
				}
			}
		}
	}
	
	override fun registerTransformers() {
		if (!PaperBinConfig.mobAiMultithreading) return
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalDoorInteract") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/BlockDoor;") {
					val list = insnBuilder {
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalDoorInteract", "a", "Lnet/minecraft/server/v1_12_R1/EntityInsentient;")
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/EntityInsentient", "world", "Lnet/minecraft/server/v1_12_R1/World;")
						+VarInsnNode(ALOAD, 1)
						+MethodInsnNode(INVOKESTATIC, AsyncMobAi::class.internalName, "betterDoorInteract", "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/BlockDoor;", false)
						+ARETURN.insn()
					}
				}
			}
		}
		register("net.minecraft.server.v1_12_R1.PathfinderGoalFollowOwner") { classNode ->
			for (method in classNode.methods) {
				for (insn in method.instructions) {
					if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
						insn.name = "getTypeIfLoaded"
						val after = insnBuilder {
							val jmp = LabelNode()
							+DUP.insn()
							+JumpInsnNode(IFNONNULL, jmp)
							+ICONST_0.insn()
							+IRETURN.insn()
							+jmp
						}
						method.instructions.insert(insn, after)
						return@register
					}
				}
			}
			error("Couldnt find target")
		}
		register("net.minecraft.server.v1_12_R1.PathfinderGoalFollowOwnerParrot") { classNode ->
			for (method in classNode.methods) {
				for (insn in method.instructions) {
					if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
						insn.name = "getTypeIfLoaded"
						val after = insnBuilder {
							val jmp = LabelNode()
							+DUP.insn()
							+JumpInsnNode(IFNONNULL, jmp)
							+ICONST_0.insn()
							+IRETURN.insn()
							+jmp
						}
						method.instructions.insert(insn, after)
						return@register
					}
				}
			}
			error("Couldnt find target")
		}
		register("net.minecraft.server.v1_12_R1.PathfinderGoalJumpOnBlock") { classNode ->
			for (method in classNode.methods) {
				for (insn in method.instructions) {
					if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
						insn.name = "getTypeIfLoaded"
						val after = insnBuilder {
							val jmp = LabelNode()
							+DUP.insn()
							+JumpInsnNode(IFNONNULL, jmp)
							+ICONST_0.insn()
							+IRETURN.insn()
							+jmp
						}
						method.instructions.insert(insn, after)
						return@register
					}
				}
			}
			error("Couldnt find target")
		}
		register("net.minecraft.server.v1_12_R1.PathfinderGoalPanic") { classNode ->
			for (method in classNode.methods) {
				for (insn in method.instructions) {
					if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
						insn.name = "getTypeIfLoaded"
						val after = insnBuilder {
							val jmp = LabelNode()
							+DUP.insn()
							+JumpInsnNode(IFNONNULL, jmp)
							+ARETURN.insn()
							+jmp
						}
						method.instructions.insert(insn, after)
						return@register
					}
				}
			}
			error("Couldnt find target")
		}
		register("net.minecraft.server.v1_12_R1.PathfinderGoalRandomFly", this::fixGetType)
		register("net.minecraft.server.v1_12_R1.PathfinderGoalVillagerFarm", this::fixGetType)
		
		register("net.minecraft.server.v1_12_R1.World") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/AxisAlignedBB;Lnet/minecraft/server/v1_12_R1/Material;)Z") {
					method.instructions.insert(insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+VarInsnNode(ALOAD, 1)
						+VarInsnNode(ALOAD, 2)
						+MethodInsnNode(INVOKESTATIC, AsyncMobAi::class.internalName, "betterIsMaterialInBB", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z", false)
						+IRETURN.insn()
					})
					return@register
				}
			}
			error("Could not find target")
		}
		
		register("net.minecraft.server.v1_12_R1.NavigationAbstract") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Z") {
					method.instructions.insert(insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/NavigationAbstract", "b", "Lnet/minecraft/server/v1_12_R1/World;")
						+VarInsnNode(ALOAD, 1)
						+MethodInsnNode(INVOKESTATIC, AsyncMobAi::class.internalName, "betterCanEntityStandOn", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false)
						+IRETURN.insn()
					})
					return@register
				}
			}
			error("Couldnt find target")
		}
		
		register("net.minecraft.server.v1_12_R1.EntityInsentient") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "doTick" && method.desc == "()V") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/PathfinderGoalSelector" && insn.name == "a" && insn.desc == "()V") {
							insn.name = "tickGoals"
							return@register
						}
					}
				}
			}
			error("Couldnt find target")
		}
	}
}
