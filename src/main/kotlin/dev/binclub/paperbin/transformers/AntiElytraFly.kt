package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperBinFeature
import net.minecraft.server.v1_12_R1.EntityPlayer
import net.minecraft.server.v1_12_R1.EnumItemSlot
import net.minecraft.server.v1_12_R1.ItemElytra
import net.minecraft.server.v1_12_R1.Items
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityToggleGlideEvent

/**
 * @author cookiedragon234 25/May/2020
 */
object AntiElytraFly: PaperBinFeature {
	override fun postStartup() {
		if (!PaperBinConfig.antiElytraFly) return
		
		Bukkit.getServer().pluginManager.registerEvent(
			EntityToggleGlideEvent::class.java,
			object: Listener {},
			EventPriority.HIGHEST,
			this::elytraDeployListener,
			PaperBinInfo.paperPlugin
		)
	}
	
	private fun elytraDeployListener(listener: Listener, event: Event) {
		if (!PaperBinConfig.antiElytraFly) return
		event as EntityToggleGlideEvent
		
		if (event.isGliding) {
			val entity = event.entity
			if (entity is CraftEntity) {
				val nmsEntity = entity.handle
				if (nmsEntity is EntityPlayer) {
					val itemStack = nmsEntity.getEquipment(EnumItemSlot.CHEST)
					if (itemStack.item === Items.cS && ItemElytra.d(itemStack)) {
						itemStack.damage += 1
					}
				}
			}
		}
	}
	
	override fun registerTransformers() {
	}
}
