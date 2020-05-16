package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.add
import dev.binclub.paperbin.utils.internalName
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 15/May/2020
 */
object AntiGrief: PaperFeature {
	val protectedBlocks by lazy {
		arrayOf(
			Material.END_GATEWAY,
			Material.ENDER_PORTAL,
			Material.ENDER_PORTAL_FRAME,
			Material.BEDROCK
		)
	}
	
	override fun postStartup() {
		if (!PaperBinConfig.antiGrief) return
		
		Bukkit.getServer().pluginManager.registerEvent(
			PlayerBucketEmptyEvent::class.java,
			object: Listener {},
			EventPriority.HIGHEST,
			this::bucketListener,
			PaperBinInfo.paperPlugin
		)
	}
	
	fun bucketListener(listener: Listener, event: Any) {
		if (!PaperBinConfig.antiGrief) return
		event as PlayerBucketEmptyEvent
		
		val relative = event.blockClicked.getRelative(event.blockFace)
		
		if (protectedBlocks.contains(relative.type)) {
			event.isCancelled = true
		}
	}
	
	override fun registerTransformers() {
		return
	}
}
