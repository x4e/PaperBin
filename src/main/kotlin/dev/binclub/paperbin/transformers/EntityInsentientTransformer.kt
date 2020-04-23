package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.internalName
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.IllegalStateException

/**
 * @author cookiedragon234 23/Apr/2020
 */
object EntityInsentientTransformer: PaperFeatureTransformer {
	override fun transformClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			if (method.name == "doTick" && method.desc == "()V") {
				for (insn in method.instructions) {
					if (insn is LdcInsnNode) {
						if (insn.cst == "sensing") {
							val load = insn.previous?.previous?.previous
							
							if (load == null) {
								IllegalStateException("Null Load").printStackTrace()
								return
							}
							
							val list = InsnList().apply {
								val out = LabelNode()
								add(MethodInsnNode(INVOKESTATIC, EntityInsentientTransformer::class.internalName, "shouldDoAi", "()Z", false))
								add(JumpInsnNode(IFNE, out))
								add(InsnNode(RETURN))
								add(out)
							}
							
							method.instructions.insertBefore(load, list)
							return
						}
					}
				}
			}
		}
		error("Couldnt find target")
	}
	
	@JvmStatic
	fun shouldDoAi(): Boolean {
		return false
	}
}
