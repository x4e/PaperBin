package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.insnBuilder
import dev.binclub.paperbin.utils.internalName
import net.minecraft.server.v1_12_R1.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode
import java.lang.reflect.Field
import kotlin.math.sqrt

/**
 * @author Tigermouthbear 6/June/2020
 */
object ElytraLowerRenderDistance: PaperFeature {
	@JvmStatic
	private val distance = 2

	override fun registerTransformers() {
		if(!PaperBinConfig.elytraLowerRenderDistance) return

		// add speed fields to entity player
		register("net.minecraft.server.v1_12_R1.EntityPlayer") { cn ->
			cn.fields.add(FieldNode(ACC_PUBLIC, "speed", "D", null, 0.0))
			cn.fields.add(FieldNode(ACC_PUBLIC, "fixedLastX", "D", null, 0.0))
			cn.fields.add(FieldNode(ACC_PUBLIC, "fixedLastZ", "D", null, 0.0))
		}

		// register entity tick listener
		register("net.minecraft.server.v1_12_R1.Entity") { cn ->
			for(mn in cn.methods) {
				if(mn.name == "Y" && mn.desc == "()V") {
					mn.instructions.insertBefore(mn.instructions.first, insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+MethodInsnNode(INVOKESTATIC, ElytraLowerRenderDistance::class.internalName, "onEntityTick", "(Lnet/minecraft/server/v1_12_R1/Entity;)V", false)
					})
					return@register
				}
			}

			error("Could not find target!")
		}

		// register view distance listeners
		register("net.minecraft.server.v1_12_R1.PlayerChunkMap") { cn ->
			for(mn in cn.methods) {
				if(mn.name == "movePlayer" && mn.desc == "(Lnet/minecraft/server/v1_12_R1/EntityPlayer;)V") {
					var target: MethodInsnNode? = null

					for(insn in mn.instructions) {
						if(target == null && insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/PlayerChunkMap" && insn.name == "getViewDistance" && insn.desc == "()I") target = insn
					}

					mn.instructions.insert(target, insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+VarInsnNode(ALOAD, 1)
						+MethodInsnNode(INVOKESTATIC, ElytraLowerRenderDistance::class.internalName, "getAlteredViewDistance", "(Lnet/minecraft/server/v1_12_R1/PlayerChunkMap;Lnet/minecraft/server/v1_12_R1/EntityPlayer;)I", false)
					})
				}

				if(mn.name == "setViewDistance" && mn.desc == "(Lnet/minecraft/server/v1_12_R1/EntityPlayer;IZ)V") {
					var target: VarInsnNode? = null

					for(insn in mn.instructions) {
						if(target == null && insn is VarInsnNode && insn.opcode == ISTORE && insn.`var` == 2) target = insn
					}

					mn.instructions.insert(target, insnBuilder {
						+VarInsnNode(ALOAD, 1)
						+VarInsnNode(ILOAD, 2)
						+MethodInsnNode(INVOKESTATIC, ElytraLowerRenderDistance::class.internalName, "setViewDistance", "(Lnet/minecraft/server/v1_12_R1/EntityPlayer;I)I", false)
						+VarInsnNode(ISTORE, 2)
					})

					return@register
				}
			}

			error("Could not find target!")
		}
	}

	@JvmStatic
	fun onEntityTick(entity: Entity) {
		if(entity is EntityPlayer) {
			val player: EntityPlayer = entity as EntityPlayer

			// grab last positions
			val lastXReflection: Field = player.javaClass.getField("fixedLastX")
			val lastX: Double = lastXReflection.getDouble(player)
			val lastZReflection: Field = player.javaClass.getField("fixedLastZ")
			val lastZ: Double = lastZReflection.getDouble(player)

			// calculate differences and speed
			val diffX: Double = player.locX - lastX
			val diffZ: Double = player.locZ - lastZ
			val speed = sqrt(diffX * diffX + diffZ * diffZ)

			// set last positions
			lastXReflection.setDouble(player, player.locX)
			lastZReflection.setDouble(player, player.locZ)

			PaperBinInfo.logger.info(speed.toString())

			// set speed
			player.javaClass.getField("speed").setDouble(player, speed)
		}
	}

	@JvmStatic
	fun getAlteredViewDistance(playerChunkMap: PlayerChunkMap, player: EntityPlayer): Int {
		val viewDistance = if(isPlayerSpeeding(player)) distance else playerChunkMap.viewDistance
		playerChunkMap.updateViewDistance(player, viewDistance)
		return viewDistance
	}

	@JvmStatic
	fun setViewDistance(player: EntityPlayer, viewDistance: Int): Int {
		return if(isPlayerSpeeding(player)) distance else viewDistance
	}

	private fun isPlayerSpeeding(player: EntityPlayer): Boolean {
		// get fields in entity player needed for calculations
		var speed: Double
		synchronized(onEntityTick(player)) {
			speed = player.javaClass.getField("speed").getDouble(player)
		}
		return speed > 0.2
	}
}