package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.insnBuilder
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * Only allows characters between ' ' to '~'
 *
 * @see https://jrgraphix.net/r/Unicode/0020-007F
 *
 * @author cookiedragon234 18/May/2020
 */
object AntiUnicodeChat: PaperBinFeature {
	@JvmStatic
	fun isAllowedCharacter(char: Char): Boolean {
		return (char in ' '..'~')
	}
	
	override fun registerTransformers() {
		if (!PaperBinConfig.antiUnicodeChat) return
		
		register("net.minecraft.server.v1_12_R1.SharedConstants") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "isAllowedChatCharacter" && method.desc == "(C)Z") {
					val list = insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+MethodInsnNode(INVOKESTATIC, "dev/binclub/paperbin/transformers/AntiUnicodeChat", "isAllowedCharacter", "(C)Z", false)
						+IRETURN.insn()
					}
					method.instructions.insert(list)
					return@register
				}
			}
			error("Couldnt find target")
		}
	}
}
