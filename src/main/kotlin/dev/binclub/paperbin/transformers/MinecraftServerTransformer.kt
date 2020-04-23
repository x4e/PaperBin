package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.internalName
import org.bukkit.Bukkit
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 23/Apr/2020
 */
object MinecraftServerTransformer: PaperFeatureTransformer {
	override fun transformClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			if (method.name == "D" && method.desc == "()V") {
				val list = InsnList().apply {
					add(VarInsnNode(ALOAD, 0))
					add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/MinecraftServer", "ticks", "I"))
					add(MethodInsnNode(INVOKESTATIC, this::class.internalName, "onServerTick", "(I)V"))
				}
			}
		}
	}
	
	@JvmStatic
	fun onServerTick(ticks: Int) {
		PaperBinInfo.ticks = ticks
	}
}
