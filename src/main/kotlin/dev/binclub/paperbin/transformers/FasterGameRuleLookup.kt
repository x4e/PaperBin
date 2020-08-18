package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode

/**
 * @author cookiedragon234 11/May/2020
 */
object FasterGameRuleLookup: PaperBinFeature {
	override fun registerTransformers() {
		if (!PaperBinConfig.fastGameRule) return
		
		register("net.minecraft.server.v1_12_R1.GameRules") { classNode ->
			for (field in classNode.fields) {
				if (field.name == "a") {
					field.desc =
						changeType(field.desc)
				}
			}
			
			for (method in classNode.methods) {
				for (insn in method.instructions) {
					when (insn) {
						is TypeInsnNode -> {
							insn.desc =
								changeType(
									insn.desc
								)
						}
						is MethodInsnNode -> {
							insn.owner =
								changeType(
									insn.owner
								)
							insn.name =
								changeType(
									insn.name
								)
							insn.desc =
								changeType(
									insn.desc
								)
						}
						is FieldInsnNode -> {
							insn.owner =
								changeType(
									insn.owner
								)
							insn.name =
								changeType(
									insn.name
								)
							insn.desc =
								changeType(
									insn.desc
								)
						}
					}
				}
			}
		}
	}
	
	private fun changeType(type: String): String {
		return type.replace("java/util/TreeMap", "java/util/HashMap")
	}
}
