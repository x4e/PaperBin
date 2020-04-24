package dev.binclub.paperbin.transformers

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode

/**
 * @author cookiedragon234 24/Apr/2020
 */
object GameRulesTransformer: PaperFeatureTransformer {
	override fun transformClass(classNode: ClassNode) {
		for (field in classNode.fields) {
			if (field.name == "a") {
				field.desc = changeType(field.desc)
			}
		}
		
		for (method in classNode.methods) {
			for (insn in method.instructions) {
				when (insn) {
					is TypeInsnNode -> {
						insn.desc = changeType(insn.desc)
					}
					is MethodInsnNode -> {
						insn.owner = changeType(insn.owner)
						insn.name = changeType(insn.name)
						insn.desc = changeType(insn.desc)
					}
					is FieldInsnNode -> {
						insn.owner = changeType(insn.owner)
						insn.name = changeType(insn.name)
						insn.desc = changeType(insn.desc)
					}
				}
			}
		}
	}
	
	private fun changeType(type: String): String {
		return type.replace("java/util/TreeMap", "java/util/HashMap")
	}
}
