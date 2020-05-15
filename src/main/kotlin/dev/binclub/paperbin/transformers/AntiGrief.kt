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
	val protectedBlocks = arrayOf(
		Material.END_GATEWAY,
		Material.ENDER_PORTAL,
		Material.ENDER_PORTAL_FRAME,
		Material.BEDROCK
	)
	
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
		register("net.minecraft.server.v1_12_R1.ItemBucket") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/EntityHuman;Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/EnumDirection;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/ItemStack;)Z") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/World" && insn.name == "isEmpty" && insn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;)Z") {
							val before = InsnList().apply {
								add(DUP)
							}
							val list = InsnList().apply {
								val tLabel = LabelNode()
								val end = LabelNode()
								add(SWAP)
								
								add(FieldInsnNode(GETSTATIC, "net/minecraft/server/v1_12_R1/Blocks", "END_GATEWAY", "Lnet/minecraft/server/v1_12_R1/Block;"))
								add(JumpInsnNode(IF_ACMPEQ, tLabel))
								add(ICONST_1)
								add(JumpInsnNode(GOTO, end))
								add(tLabel)
								add(ICONST_0)
								add(end)
								add(IAND)
							}
							method.instructions.insertBefore(insn, before)
							method.instructions.insert(insn, list)
							return@register
						}
					}
				}
			}
			error("Couldnt find target")
		}
	}
}
