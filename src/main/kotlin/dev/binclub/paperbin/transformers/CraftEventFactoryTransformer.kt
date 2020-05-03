package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperFeatureTransformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * Clone of https://github.com/PaperMC/Paper/commit/a6ac47e502d4cadef0c2c289956971739076c49c
 *
 * @author cookiedragon234 29/Apr/2020
 */
object CraftEventFactoryTransformer: PaperFeatureTransformer("org.bukkit.craftbukkit.v1_12_R1.event.CraftEventFactory") {
	override fun transformClass(classNode: ClassNode) {
		var i = 0
		for (method in classNode.methods) {
			for (insn in method.instructions) {
				if (
					insn is MethodInsnNode
					&&
					insn.owner == "org/bukkit/craftbukkit/v1_12_R1/CraftWorld"
					&&
					insn.name == "dropItemNaturally"
					&&
					insn.desc == "(Lorg/bukkit/Location;Lorg/bukkit/inventory/ItemStack;)Lorg/bukkit/entity/Item;"
				) {
					// Paper - destroy this item - if this ever leaks due to game bugs, ensure it doesn't dupe
					val before = InsnNode(DUP_X2)
					val after = InsnList().apply {
						add(InsnNode(ICONST_0))
						add(MethodInsnNode(
							INVOKEVIRTUAL,
							"org/bukkit/inventory/ItemStack",
							"setAmount",
							"(I)V",
							false
						))
					}
					method.instructions.insertBefore(insn, before)
					method.instructions.insert(insn, after)
					
					i += 1
				}
			}
		}
		
		if (i < 2) error("Couldnt find target")
	}
}
