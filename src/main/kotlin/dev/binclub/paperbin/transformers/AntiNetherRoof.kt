package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperFeature
import net.minecraft.server.v1_12_R1.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.v1_12_R1.CraftServer
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent

/**
 * @author cookiedragon234 12/May/2020
 */
object AntiNetherRoof: PaperFeature {
	override fun registerTransformers() {}
	
	override fun postStartup() {
		if (!PaperBinConfig.antiNetherRoof) return
		
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
				// If they are teleporting from another invalid location this likely means they are stuck
				// Rather than just cancelling the teleport and preventing them from moving the most
				// humane thing to do is just to kill them :/
				player.damage(20000.0)
			}
		}
	}
	
	fun isValid(location: Location): Boolean {
		val x = location.x.toInt()
		val y = location.y.toInt()
		val z = location.z.toInt()
		
		if (x >= worldBorder || z >= worldBorder || x <= negWorldBorder || z <= negWorldBorder) {
			return false
		}
		
		when (location.world.environment) {
			World.Environment.NETHER -> {
				if (y <= 0 || y >= 125) {
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
