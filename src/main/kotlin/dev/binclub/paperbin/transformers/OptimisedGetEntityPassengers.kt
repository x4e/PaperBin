package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.insnBuilder
import net.minecraft.server.v1_12_R1.Entity
import net.minecraft.server.v1_12_R1.World
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * Original mojang code:
 *
 * public List bF() {
 *  return (List)(this.passengers.isEmpty() ? Collections.emptyList() : Lists.newArrayList((Iterable)this.passengers));
 * }
 *
 * as you can see it clones the list each time which is p bad for performance
 *
 * The purpose of this is presumably to disallow other classes from directly modifying the passengers list, however
 * I couldn't find any usage of this function which modified the list.
 *
 * Plugins will still access the unmodifiable clone list, however mojangs code will now use the unsafe method
 *
 * @author cookiedragon234 19/May/2020
 */
object OptimisedGetEntityPassengers: PaperFeature {
	override fun registerTransformers() {
		register("net.minecraft.server.v1_12_R1.Entity") { classNode ->
			classNode.methods.add(MethodNode(
				ACC_PUBLIC,
				"getPassengersUnsafe",
				"()Ljava/util/List;",
				null,
				null
			).also {
				it.instructions = insnBuilder {
					+VarInsnNode(ALOAD, 0)
					+FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/Entity", "passengers", "Ljava/util/List;")
					+ARETURN.insn()
				}
			})
		}
		register("net.minecraft.server.v1_12_R1.Entity", this::useUnsafe)
		register("net.minecraft.server.v1_12_R1.EntityBoat", this::useUnsafe)
		register("net.minecraft.server.v1_12_R1.EntityHorseAbstract", this::useUnsafe)
		register("net.minecraft.server.v1_12_R1.EntityMinecartAbstract", this::useUnsafe)
		register("net.minecraft.server.v1_12_R1.EntityPig", this::useUnsafe)
		register("net.minecraft.server.v1_12_R1.EntityAIBodyControl", this::useUnsafe)
		register("net.minecraft.server.v1_12_R1.EntityTrackerEntry", this::useUnsafe)
		register("net.minecraft.server.v1_12_R1.PacketPlayOutMount", this::useUnsafe)
		register("net.minecraft.server.v1_12_R1.PathfinderGoalTame", this::useUnsafe)
		register("net.minecraft.server.v1_12_R1.World", this::useUnsafe)
		register("net.minecraft.server.v1_12_R1.ChunkRegionLoader", this::useUnsafe)
	}
	
	private fun useUnsafe(classNode: ClassNode) {
		for (method in classNode.methods) {
			for (insn in method.instructions) {
				if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/Entity" && insn.name == "bF" && insn.desc == "()Ljava/util/List;") {
					insn.name = "getPassengersUnsafe"
				}
			}
		}
	}
}
