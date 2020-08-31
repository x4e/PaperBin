package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.insnBuilder
import net.minecraft.server.v1_12_R1.BlockPosition
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM6
import org.objectweb.asm.tree.MethodNode

/**
 * @author cookiedragon234 17/Aug/2020
 */
object AntiPhysicsCrash: PaperBinFeature {
	override fun registerTransformers() {
		if (!PaperBinConfig.antiPhysicsCrash) return
		
		register("net/minecraft/server/v1_12_R1/World") { cn ->
			var overload: MethodNode? = null
			cn.methods.forEach { mn ->
				if (mn.name == "applyPhysics" && mn.desc == "(Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/Block;Z)V") {
					
					overload = MethodNode(
						mn.access,
						mn.name,
						mn.desc,
						mn.signature,
						mn.exceptions?.toTypedArray()
					).apply {
						mn.desc = "(Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/Block;ZI)V"
						
						instructions = insnBuilder {
							aload(0)
							aload(1)
							aload(2)
							iload(3)
							iconst_0()
							invokevirtual(cn.name, mn.name, mn.desc)
							_return()
						}
					}
				}
			}
			cn.methods.add(overload)
		}
	}
}
