package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperFeature
import net.minecraft.server.v1_12_R1.EntityTameableAnimal
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.lang.Math.pow
import kotlin.math.sqrt

/**
 * @author cookiedragon234 12/May/2020
 */
object AntiEntitySpeed: PaperFeature {
	override fun registerTransformers() {}
	
	override fun postStartup() {
		if (!PaperBinConfig.antiNetherRoof) return
		
		Bukkit.getServer().pluginManager.registerEvent(
			PlayerMoveEvent::class.java,
			object: Listener {},
			EventPriority.HIGHEST,
			this::moveListener,
			PaperBinInfo.paperPlugin
		)
	}
	
	private fun moveListener(listener: Listener, event: Event) {
		if (!PaperBinConfig.antiNetherRoof) return
		event as PlayerMoveEvent
		
		val player = event.player
		
		if (player is CraftPlayer) {
			val riding = player.handle.bJ()
			if (riding is EntityTameableAnimal) {
				if (riding.motX * riding.motX + riding.motZ * riding.motZ > 12.0) {
					player.handle.stopRiding()
					riding.motX = 0.0
					riding.motZ = 0.0
				}
			}
		}
	}
}
