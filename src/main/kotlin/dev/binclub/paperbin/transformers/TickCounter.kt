package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.internalName
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 11/May/2020
 */
object TickCounter: PaperFeature {
	override fun registerTransformers() {
		register("net.minecraft.server.v1_12_R1.MinecraftServer") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "D" && method.desc == "()V") {
					val list = InsnList().apply {
						add(VarInsnNode(ALOAD, 0))
						add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/MinecraftServer", "ticks", "I"))
						add(MethodInsnNode(INVOKESTATIC, TickCounter::class.internalName, "onServerTick", "(I)V", false))
					}
					method.instructions.insert(list)
					return@register
				}
			}
			error("Couldnt find target")
		}
	}
	
	@JvmStatic
	fun onServerTick(ticks: Int) {
		PaperBinInfo.ticks = ticks
		
		if (!PaperBinInfo.started) {
			PaperBinInfo.onStartup()
		}
	}
}
