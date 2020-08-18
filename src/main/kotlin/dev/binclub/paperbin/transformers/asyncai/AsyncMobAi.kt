package dev.binclub.paperbin.transformers.asyncai

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.*
import net.minecraft.server.v1_12_R1.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.reflect.InvocationTargetException
import kotlin.concurrent.thread

/**
 * @author cookiedragon234 25/May/2020
 */
object AsyncMobAi: PaperBinFeature {
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
							var t2 = t
							if (t2 is InvocationTargetException) {
								t2 = t2.cause ?: t2
							}
							IllegalStateException("Exception while updating mob AI for $entity", t2).printStackTrace()
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
		if (!PaperBinConfig.mobAiMultithreading) return
		
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalPanic") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/Entity;II)Lnet/minecraft/server/v1_12_R1/BlockPosition;") {
					val insert = insnBuilder {
						+VarInsnNode(ALOAD, 1)
						+VarInsnNode(ALOAD, 2)
						+VarInsnNode(ILOAD, 3)
						+VarInsnNode(ILOAD, 4)
						+MethodInsnNode(
							INVOKESTATIC,
							"dev/binclub/paperbin/transformers/asyncai/AsyncMobAiReplacedFunctions",
							"PathfinderGoalPanica",
							"(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/Entity;II)Lnet/minecraft/server/v1_12_R1/BlockPosition;",
							false
						)
						+ARETURN.insn()
					}
					method.instructions.insert(insert)
					return@register
				}
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalDoorInteract") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/BlockDoor;") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
							insn.name = "getTypeIfLoaded"
							val after = insnBuilder {
								val jmp = LabelNode()
								+DUP.insn()
								+JumpInsnNode(IFNONNULL, jmp)
								+ACONST_NULL.insn()
								+ARETURN.insn()
								+jmp
							}
							method.instructions.insert(insn, after)
							return@register
						}
					}
				}
			}
			error("Couldn't find target")
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
			error("Couldn't find target")
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
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalJumpOnBlock") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;)Z") {
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
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalRandomFly") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "j" && method.desc == "()Lnet/minecraft/server/v1_12_R1/Vec3D;") {
					val insert = insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalRandomFly", "a", "Lnet/minecraft/server/v1_12_R1/EntityCreature;")
						+MethodInsnNode(
							INVOKESTATIC,
							"dev/binclub/paperbin/transformers/asyncai/AsyncMobAiReplacedFunctions",
							"PathfinderGoalRandomFlyj",
							"(Lnet/minecraft/server/v1_12_R1/EntityCreature;)Lnet/minecraft/server/v1_12_R1/Vec3D;",
							false
						)
						+ARETURN.insn()
					}
					method.instructions.insert(insert)
					return@register
				}
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalVillagerFarm") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;)Z") {
					var i = 0
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
							if (i == 0) {
								insn.name = "getTypeIfLoaded"
								i = 1
							} else if (i == 2) {
								insn.name = "getTypeIfLoaded"
								i = 3
							}
						} else if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/IBlockData" && insn.name == "getBlock" && insn.desc == "()Lnet/minecraft/server/v1_12_R1/Block;") {
							if (i == 1) {
								val after = LabelNode()
								val before = insnBuilder {
									+DUP.insn()
									+JumpInsnNode(IFNULL, after)
								}
								method.instructions.insertBefore(insn, before)
								method.instructions.insert(insn, after)
								i = 2
							} else if (i == 3) {
								val after = LabelNode()
								val before = insnBuilder {
									+DUP.insn()
									+JumpInsnNode(IFNULL, after)
								}
								method.instructions.insertBefore(insn, before)
								method.instructions.insert(insn, after)
								i = 4
							}
						} else if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/IBlockData" && insn.name == "getMaterial" && insn.desc == "()Lnet/minecraft/server/v1_12_R1/Material;") {
							if (i == 4) {
								val after = LabelNode()
								val before = insnBuilder {
									+DUP.insn()
									+JumpInsnNode(IFNULL, after)
								}
								method.instructions.insertBefore(insn, before)
								method.instructions.insert(insn, after)
								i = 5
							}
						}
					}
					if (i == 5) {
						return@register
					} else {
						error("Could not find target $i")
					}
				}
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.World") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/AxisAlignedBB;Lnet/minecraft/server/v1_12_R1/Material;)Z") {
					method.instructions.insert(insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+VarInsnNode(ALOAD, 1)
						+VarInsnNode(ALOAD, 2)
						+MethodInsnNode(
							INVOKESTATIC,
							"dev/binclub/paperbin/transformers/asyncai/AsyncMobAiReplacedFunctions",
							"Worlda",
							"(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/AxisAlignedBB;Lnet/minecraft/server/v1_12_R1/Material;)Z",
							false
						)
						+IRETURN.insn()
					})
					return@register
				}
			}
			error("Could not find target")
		}
		
		register("net.minecraft.server.v1_12_R1.NavigationAbstract") { classNode ->
			var i = 0
			for (method in classNode.methods) {
				if (method.name == "q_" && method.desc == "()V") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
							insn.name = "getTypeIfLoaded"
							i += 1
						} else if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/IBlockData" && insn.name == "getBlock" && insn.desc == "()Lnet/minecraft/server/v1_12_R1/Block;") {
							val after = LabelNode()
							val before = insnBuilder {
								+DUP.insn()
								+JumpInsnNode(IFNULL, after)
							}
							method.instructions.insertBefore(insn, before)
							method.instructions.insert(insn, after)
							i += 1
						}
					}
				}
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Z") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
							insn.name = "getTypeIfLoaded"
							val after = insnBuilder {
								val lbl = LabelNode()
								+DUP.insn()
								+JumpInsnNode(IFNONNULL, lbl)
								+ICONST_0.insn()
								+IRETURN.insn()
								+lbl
							}
							method.instructions.insert(insn, after)
							i += 1
						}
					}
				}
			}
			if (i != 3) {
				error("Couldnt find target $i")
			}
		}
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalEatTile") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "e" && method.desc == "()V") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
							insn.name = "getTypeIfLoaded"
							val after = insnBuilder {
								val jmp = LabelNode()
								+DUP.insn()
								+JumpInsnNode(IFNONNULL, jmp)
								+RETURN.insn()
								+jmp
							}
							method.instructions.insert(insn, after)
							return@register
						}
					}
				}
			}
			error("Couldnt find target")
		}
		
		register("net.minecraft.server.v1_12_R1.EntityRabbit\$PathfinderGoalEatCarrots") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;)Z"){
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
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalMoveIndoors") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "d" && method.desc == "()V") {
					val insert = insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalMoveIndoors", "b", "Lnet/minecraft/server/v1_12_R1/VillageDoor;")
						val jmp = LabelNode()
						+JumpInsnNode(IFNONNULL, jmp)
						+RETURN.insn()
						+jmp
					}
					method.instructions.insert(insert)
					return@register
				}
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.Village") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "c" && method.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/VillageDoor;") {
					var count = 0
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
							insn.name = "getTypeIfLoaded"
							val after = insnBuilder {
								val jmp = LabelNode()
								+DUP.insn()
								+JumpInsnNode(IFNONNULL, jmp)
								+ACONST_NULL.insn()
								+ARETURN.insn()
								+jmp
							}
							method.instructions.insert(insn, after)
							count += 1
						}
					}
					if (count >= 4) {
						return@register
					} else {
						error("Couldn't find target $count")
					}
				}
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalLookAtPlayer") { classNode ->
			var count = 0
			for (method in classNode.methods) {
				if (method.name == "b" && method.desc == "()Z") {
					for (insn in method.instructions) {
						if (insn is FieldInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/PathfinderGoalLookAtPlayer" && insn.name == "b" && insn.desc == "Lnet/minecraft/server/v1_12_R1/Entity;") {
							val after = insnBuilder {
								val jmp = LabelNode()
								+DUP.insn()
								+JumpInsnNode(IFNONNULL, jmp)
								+ICONST_0.insn()
								+IRETURN.insn()
								+jmp
							}
							method.instructions.insert(insn, after)
							count += 1
						}
					}
				} else if (method.name == "e" && method.desc == "()V") {
					val insert = insnBuilder {
						val out = LabelNode()
						val `in` = LabelNode()
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalLookAtPlayer", "a", "Lnet/minecraft/server/v1_12_R1/EntityInsentient;")
						+JumpInsnNode(IFNULL, `in`)
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalLookAtPlayer", "b", "Lnet/minecraft/server/v1_12_R1/Entity;")
						+JumpInsnNode(IFNULL, `in`)
						+JumpInsnNode(GOTO, out)
						+`in`
						+VarInsnNode(ALOAD, 0)
						+DUP.insn()
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalLookAtPlayer", "e", "I")
						+ICONST_1.insn()
						+ISUB.insn()
						+FieldInsnNode(PUTFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalLookAtPlayer", "e", "I")
						+RETURN.insn()
						+out
					}
					method.instructions.insert(insert)
					count += 1
				}
				
				if (count >= 2) {
					return@register
				}
			}
			error("Couldnt find target $count")
		}
		
		register("net.minecraft.server.v1_12_R1.PersistentVillage") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "getClosestVillage" && method.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;I)Lnet/minecraft/server/v1_12_R1/Village;") {
					val insert = insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PersistentVillage", "villages", "Ljava/util/List;")
						+VarInsnNode(ALOAD, 1)
						+VarInsnNode(ILOAD, 2)
						+MethodInsnNode(
							INVOKESTATIC,
							"dev/binclub/paperbin/transformers/asyncai/AsyncMobAiReplacedFunctions",
							"PersistentVillagegetClosestVillage",
							"(Ljava/util/List;Lnet/minecraft/server/v1_12_R1/BlockPosition;I)Lnet/minecraft/server/v1_12_R1/Village;",
							false
						)
						+ARETURN.insn()
					}
					method.instructions.insert(insert)
					return@register
				}
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalOpenDoor") { classNode ->
			val field = FieldNode(
				ACC_PUBLIC,
				"desiredDoorState",
				"Ljava/lang/Boolean;",
				null,
				null
			)
			classNode.fields.add(field)
			
			var count = 0
			for (method in classNode.methods) {
				if (method.name == "c" && method.desc == "()V") {
					val newList = insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+ldcInt(20)
						+FieldInsnNode(PUTFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalOpenDoor", "h", "I")
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;")
						+FieldInsnNode(PUTFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalOpenDoor", field.name, field.desc)
						+RETURN.insn()
					}
					method.instructions.insert(newList)
					count += 1
				} else if (method.name == "d" && method.desc == "()V") {
					val newList = insnBuilder {
						val l1 = LabelNode()
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalOpenDoor", "g", "Z")
						+JumpInsnNode(IFEQ, l1)
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETSTATIC, "java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;")
						+FieldInsnNode(PUTFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalOpenDoor", field.name, field.desc)
						+l1
						+RETURN.insn()
					}
					method.instructions.insert(newList)
					count += 1
				} else if (method.name == "e" && method.desc == "()V") {
					val newList = insnBuilder {
						val l1 = LabelNode()
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalOpenDoor", field.name, field.desc)
						+DUP.insn()
						+VarInsnNode(ASTORE, 1)
						+JumpInsnNode(IFNULL, l1)
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalOpenDoor", "c", "Lnet/minecraft/server/v1_12_R1/BlockDoor;")
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalOpenDoor", "a", "Lnet/minecraft/server/v1_12_R1/EntityInsentient;")
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/EntityInsentient", "world", "Lnet/minecraft/server/v1_12_R1/World;")
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalOpenDoor", "b", "Lnet/minecraft/server/v1_12_R1/BlockPosition;")
						+VarInsnNode(ALOAD, 1)
						+MethodInsnNode(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false)
						+MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/server/v1_12_R1/BlockDoor", "setDoor", "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;Z)V", false)
						+VarInsnNode(ALOAD, 0)
						+ACONST_NULL.insn()
						+FieldInsnNode(PUTFIELD, "net/minecraft/server/v1_12_R1/PathfinderGoalOpenDoor", field.name, field.desc)
						+l1
					}
					method.instructions.insert(newList)
					count += 1
				}
			}
			if (count < 3) {
				error("Couldn't find target $count")
			}
		}
		
		register("net.minecraft.server.v1_12_R1.PathfinderNormal") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(IIIIDLnet/minecraft/server/v1_12_R1/EnumDirection;)Lnet/minecraft/server/v1_12_R1/PathPoint;") {
					val insert = insnBuilder {
						val l1 = LabelNode()
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderNormal", "a", "Lnet/minecraft/server/v1_12_R1/IBlockAccess;")
						+JumpInsnNode(IFNONNULL, l1)
						+ACONST_NULL.insn()
						+ARETURN.insn()
						+l1
					}
					method.instructions.insert(insert)
					for (insn in method.instructions) {
						if (insn is FieldInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/PathfinderNormal" && insn.name == "b" && insn.desc == "Lnet/minecraft/server/v1_12_R1/EntityInsentient;") {
							val after = insnBuilder {
								val jmp = LabelNode()
								+DUP.insn()
								+JumpInsnNode(IFNONNULL, jmp)
								+ACONST_NULL.insn()
								+ARETURN.insn()
								+jmp
							}
							method.instructions.insert(insn, after)
							return@register
						}
					}
				}
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.World") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "getTypeIfLoaded" && method.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "getCapturedBlockType" && insn.desc == "(III)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
							val replacement = insnBuilder {
								+VarInsnNode(ALOAD, 0)
								+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/World", "capturedBlockStates", "Ljava/util/ArrayList;")
								+MethodInsnNode(
									INVOKESTATIC,
									"dev/binclub/paperbin/transformers/asyncai/AsyncMobAiReplacedFunctions",
									"WorldgetCapturedBlockType",
									"(IIILjava/util/ArrayList;)Lnet/minecraft/server/v1_12_R1/IBlockData;",
									false
								)
								+InsnNode(SWAP)
								+InsnNode(POP)
							}
							method.instructions.insert(insn, replacement)
							method.instructions.remove(insn)
							return@register
						}
					}
				}
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.World") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/Entity;Lnet/minecraft/server/v1_12_R1/AxisAlignedBB;ZLjava/util/List;)Z") {
					val list = insnBuilder {
						+VarInsnNode(ALOAD, 1)
						+VarInsnNode(ALOAD, 2)
						+VarInsnNode(ILOAD, 3)
						+VarInsnNode(ALOAD, 4)
						+VarInsnNode(ALOAD, 0)
						+MethodInsnNode(
							INVOKESTATIC,
							"dev/binclub/paperbin/transformers/asyncai/AsyncMobAiReplacedFunctions",
							"Worlda",
							"(Lnet/minecraft/server/v1_12_R1/Entity;Lnet/minecraft/server/v1_12_R1/AxisAlignedBB;ZLjava/util/List;Lnet/minecraft/server/v1_12_R1/World;)Z",
							false
						)
						+IRETURN.insn()
					}
					method.instructions.insert(list)
					return@register
				}
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.PathfinderNormal") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "b" && method.desc == "()Lnet/minecraft/server/v1_12_R1/PathPoint;") {
					val varI = method.maxLocals
					method.maxLocals += 1
					val insert = insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/PathfinderAbstract", "b", "Lnet/minecraft/server/v1_12_R1/EntityInsentient;")
						+DUP.insn()
						+VarInsnNode(ASTORE, varI)
						val jmp = LabelNode()
						+JumpInsnNode(IFNONNULL, jmp)
						+ACONST_NULL.insn()
						+ARETURN.insn()
						+jmp
					}
					method.instructions.insert(insert)
					
					var count = 0
					var dontDoFirst = true
					for (insn in method.instructions) {
						if (insn is FieldInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/PathfinderAbstract" && insn.name == "b" && insn.desc == "Lnet/minecraft/server/v1_12_R1/EntityInsentient;") {
							if (dontDoFirst) { // First instance is from the code we just added above, dont remove that!
								dontDoFirst = false
								continue
							}
							val replacement = insnBuilder {
								+POP.insn()
								+VarInsnNode(ALOAD, varI)
							}
							method.instructions.insert(insn, replacement)
							method.instructions.remove(insn)
							count += 1
						}
					}
					if (count < 15) { // there are more than 15 but whatever, if it found 15 its probably found them all
						return@register
					} else {
						error("Couldn't find target $count")
					}
				}
			}
			error("Couldn't find target")
		}
		
		register("net.minecraft.server.v1_12_R1.BlockFence") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/IBlockAccess;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/EnumDirection;)Z") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/IBlockAccess" && insn.name == "getType" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;") {
							insn.opcode = INVOKESTATIC
							insn.owner = "dev/binclub/paperbin/transformers/asyncai/AsyncMobAi"
							insn.name = "blockAccessGetTypeIfLoaded"
							insn.desc = "(Lnet/minecraft/server/v1_12_R1/IBlockAccess;Lnet/minecraft/server/v1_12_R1/BlockPosition;)Lnet/minecraft/server/v1_12_R1/IBlockData;"
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
			}
			error("Couldn't find target")
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
			error("Couldn't find target")
		}
	}
	
	@JvmStatic
	fun blockAccessGetTypeIfLoaded(iBlockAcesss: IBlockAccess, position: BlockPosition): IBlockData? {
		return when (iBlockAcesss) {
			is World -> iBlockAcesss.getTypeIfLoaded(position)
			is ChunkCache -> iBlockAcesss.getType(position) // chunk cache doesnt trigger chunk loads
			else -> iBlockAcesss.getType(position)
		}
	}
}
