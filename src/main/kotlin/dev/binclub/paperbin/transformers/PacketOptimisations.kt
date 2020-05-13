package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.add
import net.minecraft.server.v1_12_R1.MCUtil
import net.minecraft.server.v1_12_R1.NetworkManager
import org.objectweb.asm.Opcodes.ICONST_1
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode
import java.lang.invoke.MethodHandles

/**
 * @author cookiedragon234 13/May/2020
 */
object PacketOptimisations: PaperFeature {
	val m by lazy {
		NetworkManager::class.java.getDeclaredMethod("m").also {
			it.isAccessible = true
		}
	}
	
	@JvmStatic
	fun asyncQueueFlush(networkManager: NetworkManager) {
		if (!PaperBinConfig.packetOptimisations) {
			m.invoke(networkManager)
		} else {
			MCUtil.scheduleAsyncTask {
				m.invoke(networkManager)
			}
		}
	}
	
	override fun registerTransformers() {
		
		register("net.minecraft.server.v1_12_R1.NetworkManager") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "a" && method.desc == "()V") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/NetworkManager" && insn.name == "m" && insn.desc == "()Z") {
							val list = InsnList().apply {
								add(MethodInsnNode(INVOKESTATIC, "dev/binclub/paperbin/transformers/PacketOptimisations", "asyncQueueFlush", "(Lnet/minecraft/server/v1_12_R1/NetworkManager;)V", false))
								add(ICONST_1)
							}
							method.instructions.insertBefore(insn, list)
							method.instructions.remove(insn)
							
							return@register
						}
					}
				}
			}
			error("Couldnt find target")
		}
	}
}
