package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperFeatureTransformer
import net.minecraft.server.v1_12_R1.EntityLiving
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 30/Apr/2020
 */
object EntityLivingTransformer: PaperFeatureTransformer("net.minecraft.server.v1_12_R1.EntityLiving") {
	override fun transformClass(classNode: ClassNode) {
		classNode.fields.add(FieldNode(
			ACC_PROTECTED,
			"eatStartTime",
			"J",
			null,
			0L
		))
		classNode.fields.add(FieldNode(
			ACC_PROTECTED,
			"totalEatTimeTicks",
			"I",
			null,
			0
		))
		
		for (method in classNode.methods) {
			if (method.name == "cI" && method.desc == "()V") {
				for (insn in method.instructions) {
					if (insn is FieldInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/EntityLiving" && insn.name == "bp" && insn.desc == "I") {
						val jump = insn.next as JumpInsnNode
						val target = jump.label
						
						val endJump = LabelNode()
						
						val list = InsnList().apply {
							add(VarInsnNode(ALOAD, 0))
							add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/EntityLiving", "activeItem", "Lnet/minecraft/server/v1_12_R1/ItemStack;"))
							add(MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/server/v1_12_R1/ItemStack", "getItem", "()Lnet/minecraft/server/v1_12_R1/Item;"))
							add(TypeInsnNode(INSTANCEOF, "net/minecraft/server/v1_12_R1/ItemFood"))
							add(JumpInsnNode(IFNE, endJump)) // Only run for food items
							add(VarInsnNode(ALOAD, 0))
							add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/EntityLiving", "eatStartTime", "J"))
							add(InsnNode(ICONST_M1))
							add(InsnNode(I2L))
							add(JumpInsnNode(IF_))
						}
					}
				}
			}
		}
		error("Couldnt find target")
	}
}
