package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
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
			var count = 0
			for (method in classNode.methods) {
				if (method.name == "D" && method.desc == "()V") {
					val list = InsnList().apply {
						add(VarInsnNode(ALOAD, 0))
						add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/MinecraftServer", "ticks", "I"))
						add(MethodInsnNode(INVOKESTATIC, TickCounter::class.internalName, "onServerTick", "(I)V", false))
					}
					method.instructions.insert(list)
					count += 1
				}
				if (method.name == "run" && method.desc == "()V") {
					for (insn in method.instructions) {
						if (insn is LdcInsnNode && insn.cst == "1.12.2") {
							insn.cst = "Paper Bin 1.12.2"
							count += 1
							break
						}
					}
				}
			}
			if (count != 2) {
				error("Couldnt find target")
			}
		}
	}
	
	@JvmStatic
	fun onServerTick(ticks: Int) {
		PaperBinInfo.ticks = ticks
		if (PaperBinConfig.debug) {
			Thread.sleep(500) // simulate low tps enviroment
		}
		
		if (!PaperBinInfo.started) {
			PaperBinInfo.onStartup()
		}
	}
}
