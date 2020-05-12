package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperFeature
import net.minecraft.server.v1_12_R1.Blocks
import net.minecraft.server.v1_12_R1.MinecraftServer
import net.minecraft.server.v1_12_R1.PlayerConnection
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.v1_12_R1.CraftServer
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

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
	}
	
	private const val worldBorder = 29999984
	private const val negWorldBorder = -29999984
	
	private fun moveListener(listener: Listener, event: Event) {
		if (!PaperBinConfig.antiNetherRoof) return
		
		event as PlayerMoveEvent
		
		val player = event.player
		val from = event.from
		val to = event.to
		
		if (player.isOp) {
			return
		}
		
		fun isValid(location: Location): Boolean {
			val x = location.x.toInt()
			val y = location.y.toInt()
			val z = location.z.toInt()
			
			if (x >= worldBorder || z >= worldBorder || x <= negWorldBorder || z <= negWorldBorder) {
				return false
			}
			
			when (to.world.environment) {
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
			}
			return true
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
}
