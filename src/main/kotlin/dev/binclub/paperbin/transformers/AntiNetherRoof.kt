package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.insnBuilder
import net.minecraft.server.v1_12_R1.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.TypeInsnNode

/**
 * @author cookiedragon234 12/May/2020
 */
object AntiNetherRoof: PaperBinFeature {
	/**
	 * Returns true if crystal can be placed here
	 */
	@JvmStatic
	fun checkCrystalPlace(world: net.minecraft.server.v1_12_R1.World, pos: BlockPosition): Boolean {
		return isValid(Location(world.world, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5))
	}
	
	override fun registerTransformers() {
		if (!PaperBinConfig.antiNetherRoof) return
		
		register("net.minecraft.server.v1_12_R1.ItemEndCrystal") { cn ->
			cn.methods.forEach { mn ->
				if (mn.name == "a" && mn.desc == "(Lnet/minecraft/server/v1_12_R1/EntityHuman;Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/EnumHand;Lnet/minecraft/server/v1_12_R1/EnumDirection;FFF)Lnet/minecraft/server/v1_12_R1/EnumInteractionResult;") {
					for (insn in mn.instructions) {
						if (
							insn is TypeInsnNode
							&&
							insn.opcode == NEW
							&&
							insn.desc == "net/minecraft/server/v1_12_R1/EntityEnderCrystal"
						) {
							val insert = insnBuilder {
								aload(2) // world
								aload(3) // pos
								invokestatic(
									"dev/binclub/paperbin/transformers/AntiNetherRoof",
									"checkCrystalPlace",
									"(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;)Z"
								)
								val out = LabelNode()
								ifne(out) // if true
								getstatic(
									"net/minecraft/server/v1_12_R1/EnumInteractionResult",
									"FAIL",
									"Lnet/minecraft/server/v1_12_R1/EnumInteractionResult;"
								)
								areturn()
								+out
							}
							mn.instructions.insertBefore(insn, insert)
							return@register
						}
					}
				}
			}
			error("Couldn't find target")
		}
	}

	private var yLevel = 0;
	
	override fun postStartup() {
		if (!PaperBinConfig.antiNetherRoof) return

		yLevel = PaperBinConfig.antiNetherRoofLevel

		Bukkit.getServer().pluginManager.registerEvent(
			PlayerMoveEvent::class.java,
			object: Listener{},
			EventPriority.HIGHEST,
			this::moveListener,
			PaperBinInfo.paperPlugin
		)
		
		Bukkit.getServer().pluginManager.registerEvent(
			VehicleEnterEvent::class.java,
			object: Listener{},
			EventPriority.HIGHEST,
			this::vehicleEnterListener,
			PaperBinInfo.paperPlugin
		)
		
		Bukkit.getServer().pluginManager.registerEvent(
			VehicleExitEvent::class.java,
			object: Listener{},
			EventPriority.HIGHEST,
			this::vehicleExitListener,
			PaperBinInfo.paperPlugin
		)
		
		Bukkit.getServer().pluginManager.registerEvent(
			BlockPlaceEvent::class.java,
			object: Listener{},
			EventPriority.HIGHEST,
			this::blockPlaceListener,
			PaperBinInfo.paperPlugin
		)
	}
	
	private const val worldBorder = 29999984
	private const val negWorldBorder = -29999984
	
	private fun blockPlaceListener(listener: Listener, event: Event) {
		if (!PaperBinConfig.antiNetherRoof) return
		event as BlockPlaceEvent
		
		val player = event.player
		val blockPos = event.blockReplacedState.location
		if (player is Player && !player.isOp) {
			if (!isValid(blockPos)) {
				event.isCancelled = true
			}
		}
	}
	
	private fun vehicleEnterListener(listener: Listener, event: Event) {
		if (!PaperBinConfig.antiNetherRoof) return
		event as VehicleEnterEvent
		
		val player = event.entered
		val vehicle = event.vehicle
		
		if (player is Player && !player.isOp) {
			if (!isValid(vehicle.location)) {
				event.isCancelled = true
			}
		}
	}
	
	private fun vehicleExitListener(listener: Listener, event: Event) {
		if (!PaperBinConfig.antiNetherRoof) return
		event as VehicleExitEvent
		
		val player = event.exited
		val vehicle = event.vehicle
		
		if (player is Player && !player.isOp) {
			if (!isValid(vehicle.location)) {
				event.isCancelled = true
			}
		}
	}
	
	private fun moveListener(listener: Listener, event: Event) {
		if (!PaperBinConfig.antiNetherRoof) return
		event as PlayerMoveEvent
		
		val player = event.player
		val from = event.from
		val to = event.to
		
		if (player.isOp) {
			return
		}
		
		if (!isValid(to)) {
			event.isCancelled = true
			if (!isValid(from)) {
				/*
					If they are teleporting from another invalid location this likely means they are stuck
					Rather than just cancelling the teleport and preventing them from moving the most
				 	humane thing to do is just to kill them :/
				 */
				player.damage(20000.0)
			}
		}
	}
	
	fun isValid(location: Location): Boolean {
		val x = location.x.toInt()
		val y = location.y.toInt()
		val z = location.z.toInt()

		if (PaperBinConfig.antiWorldBorder && (x >= worldBorder || z >= worldBorder || x <= negWorldBorder || z <= negWorldBorder)) {
			return false
		}

		val env = location.world.environment ?: return false

		when (env) {
			World.Environment.NETHER -> {
				if (y <= 0 || y >= yLevel) {
					return false
				}
			}
			World.Environment.NORMAL -> {
				if (y <= 0) {
					return false
				}
			}
			World.Environment.THE_END -> {} // No ceiling or floor
		}
		return true
	}
}
