package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.add
import dev.binclub.paperbin.utils.ldcInt
import dev.binclub.paperbin.utils.printlnAsm
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * Compensates food eating time based on TPS
 *
 * @author cookiedragon234 11/May/2020
 */
object FoodTpsCompensator: PaperFeature {
	override fun registerTransformers() {
		register("net.minecraft.server.v1_12_R1.EntityLiving") { classNode ->
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
			
			var done = 0
			
			for (method in classNode.methods) {
				if (method.name == "c" && method.desc == "(Lnet/minecraft/server/v1_12_R1/EnumHand;)V") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/ItemStack" && insn.name == "m" && insn.desc == "()I") {
							val list = InsnList().apply {
								add(VarInsnNode(ALOAD, 0))
								add(SWAP)
								add(DUP_X1)
								add(FieldInsnNode(PUTFIELD, "net/minecraft/server/v1_12_R1/EntityLiving", "totalEatTimeTicks", "I"))
								
								add(VarInsnNode(ALOAD, 0))
								add(MethodInsnNode(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false))
								add(FieldInsnNode(PUTFIELD, "net/minecraft/server/v1_12_R1/EntityLiving", "eatStartTime", "J"))
							}
							method.instructions.insert(insn, list)
							
							done += 1
						}
					}
				}
				if (method.name == "cN" && method.desc == "()V") {
					val list = InsnList().apply {
						add(VarInsnNode(ALOAD, 0))
						add(ICONST_0)
						add(FieldInsnNode(
							PUTFIELD,
							"net/minecraft/server/v1_12_R1/EntityLiving",
							"totalEatTimeTicks",
							"I"
						))
						add(VarInsnNode(ALOAD, 0))
						add(ICONST_M1)
						add(I2L)
						add(FieldInsnNode(PUTFIELD, "net/minecraft/server/v1_12_R1/EntityLiving", "eatStartTime", "J"))
					}
					
					method.instructions.insert(list)
					
					done += 1
				}
				if (method.name == "cI" && method.desc == "()V") {
					for (insn in method.instructions) {
						if (insn.previous?.opcode == DUP_X1 && insn is FieldInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/EntityLiving" && insn.name == "bp" && insn.desc == "I") {
							val falseJump = LabelNode()
							val endJump = LabelNode()
							val list = InsnList().apply {
								add(VarInsnNode(ALOAD, 0))
								add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/EntityLiving", "activeItem", "Lnet/minecraft/server/v1_12_R1/ItemStack;"))
								add(MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/server/v1_12_R1/ItemStack", "getItem", "()Lnet/minecraft/server/v1_12_R1/Item;"))
								add(TypeInsnNode(INSTANCEOF, "net/minecraft/server/v1_12_R1/ItemFood"))
								add(JumpInsnNode(IFEQ, falseJump)) // Only run for food items
								
								add(VarInsnNode(ALOAD, 0))
								add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/EntityLiving", "eatStartTime", "J"))
								add(InsnNode(ICONST_M1))
								add(InsnNode(I2L))
								add(LCMP)
								add(JumpInsnNode(IFEQ, falseJump)) // If we have started eating
								
								add(MethodInsnNode(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false))
								add(VarInsnNode(ALOAD, 0))
								add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/EntityLiving", "eatStartTime", "J"))
								add(InsnNode(LSUB))
								add(VarInsnNode(ALOAD, 0))
								add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/EntityLiving", "totalEatTimeTicks", "I"))
								add(ICONST_1)
								add(IADD)
								add(ldcInt(1000 * 1000 * 50))
								add(IMUL)
								add(I2L)
								add(LCMP)
								add(JumpInsnNode(IFLE, falseJump)) // If we have been eating for longer than the needed eat time
								
								add(InsnNode(ICONST_1))
								add(JumpInsnNode(GOTO, endJump))
								
								add(falseJump)
								add(InsnNode(ICONST_0))
								add(endJump)
								add(InsnNode(IOR))
								add(ICONST_1)
								add(IXOR)
							}
							method.instructions.insert(insn, list)
							
							done += 1
						}
						if (insn is MethodInsnNode && insn.name == "v" && insn.desc == "()V") {
							val list = InsnList().apply {
								add(VarInsnNode(ALOAD, 0))
								add(ICONST_0)
								add(FieldInsnNode(PUTFIELD, "net/minecraft/server/v1_12_R1/EntityLiving", "bp", "I"))
							}
							method.instructions.insert(insn, list)
							done += 1
						}
					}
				}
			}
			if (done != 4) {
				error("Couldnt find target $done")
			}
		}
	}
}
