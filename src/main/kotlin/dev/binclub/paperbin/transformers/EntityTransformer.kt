package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperFeatureTransformer
import net.minecraft.server.v1_12_R1.EntityItem
import org.objectweb.asm.Opcodes.ICONST_0
import org.objectweb.asm.Opcodes.INVOKEVIRTUAL
import org.objectweb.asm.tree.*

/**
 * Clone of https://github.com/PaperMC/Paper/commit/a6ac47e502d4cadef0c2c289956971739076c49c
 *
 * @author cookiedragon234 29/Apr/2020
 */
object EntityTransformer: PaperFeatureTransformer("net.minecraft.server.v1_12_R1.Entity") {
	override fun transformClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/ItemStack;F)Lnet/minecraft/server/v1_12_R1/EntityItem;") {
				for (insn in method.instructions) {
					if (insn is MethodInsnNode && insn.name == "asBukkitCopy") {
						insn.name = "asCraftMirror"
					} else if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/EntityItem" && insn.name == "<init>") {
						val prev = insn.previous
						if (prev is VarInsnNode) {
							val before = MethodInsnNode(
								INVOKEVIRTUAL,
								"net/minecraft/server/v1_12_R1/ItemStack",
								"cloneItemStack",
								"()Lnet/minecraft/server/v1_12_R1/ItemStack;",
								false
							)
							val after = InsnList().apply {
								add(VarInsnNode(prev.opcode, prev.`var`))
								add(InsnNode(ICONST_0))
								// Paper - destroy this item - if this ever leaks due to game bugs, ensure it doesn't dupe
								add(MethodInsnNode(
									INVOKEVIRTUAL,
									"net/minecraft/server/v1_12_R1/ItemStack",
									"setCount",
									"(I)V",
									false
								))
							}
							
							method.instructions.insertBefore(insn, before)
							method.instructions.insert(insn, after)
						}
					}
				}
			}
		}
	}
}
