package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.add
import dev.binclub.paperbin.utils.internalName
import dev.binclub.paperbin.utils.ldcInt
import net.minecraft.server.v1_12_R1.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * Compensates food eating time based on TPS
 *
 * @author cookiedragon234 11/May/2020
 */
object FoodTpsCompensator: PaperFeature {
	@JvmStatic
	fun getPerfectCurrentTick(): Int {
		// MinecraftServer.currentTick is affected by TPS (obviously)
		// This function returns what the current tick would be if the server was running at a constant 20tps
		return ((System.nanoTime() - PaperBinInfo.serverStartTime) / 50000000).toInt()
	}
	
	override fun registerTransformers() {
		if (!PaperBinConfig.foodTpsCompensate) return
		
		register("net.minecraft.server.v1_12_R1.PlayerConnection") { classNode ->
			var count = 0
			
			for (method in classNode.methods) {
				for (insn in method.instructions) {
					if (insn is FieldInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/MinecraftServer" && insn.name == "currentTick" && insn.desc == "I") {
						val new = MethodInsnNode(INVOKESTATIC, FoodTpsCompensator::class.internalName, "getPerfectCurrentTick", "()I", false)
						
						method.instructions.insert(insn, new)
						method.instructions.remove(insn)
						
						count += 1
					}
				}
			}
			
			if (count < 9) {
				error("Couldnt find target $count")
			}
		}
		register("net.minecraft.server.v1_12_R1.EntityItem") { classNode ->
			var count = 0
			
			for (method in classNode.methods) {
				for (insn in method.instructions) {
					if (insn is FieldInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/MinecraftServer" && insn.name == "currentTick" && insn.desc == "I") {
						val new = MethodInsnNode(INVOKESTATIC, FoodTpsCompensator::class.internalName, "getPerfectCurrentTick", "()I", false)
						
						method.instructions.insert(insn, new)
						method.instructions.remove(insn)
						
						count += 1
					}
				}
			}
			
			if (count < 6) {
				error("Couldnt find target $count")
			}
		}
		register("net.minecraft.server.v1_12_R1.PlayerInteractManager") { classNode ->
			var count = 0
			
			for (method in classNode.methods) {
				for (insn in method.instructions) {
					if (insn is FieldInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/MinecraftServer" && insn.name == "currentTick" && insn.desc == "I") {
						val new = MethodInsnNode(INVOKESTATIC, FoodTpsCompensator::class.internalName, "getPerfectCurrentTick", "()I", false)
						
						method.instructions.insert(insn, new)
						method.instructions.remove(insn)
						
						count += 1
					}
				}
			}
			
			if (count < 2) {
				error("Couldnt find target $count")
			}
		}
		register("net.minecraft.server.v1_12_R1.TileEntityFurnace") { classNode ->
			var count = 0
			for (method in classNode.methods) {
				for (insn in method.instructions) {
					if (insn is FieldInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/MinecraftServer" && insn.name == "currentTick" && insn.desc == "I") {
						val new = MethodInsnNode(INVOKESTATIC, FoodTpsCompensator::class.internalName, "getPerfectCurrentTick", "()I", false)
						
						method.instructions.insert(insn, new)
						method.instructions.remove(insn)
						
						count += 1
					}
				}
			}
			if (count < 1) {
				error("Couldnt find target $count")
			}
		}
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
							val jump = insn.next as JumpInsnNode
							val afterJump = LabelNode()
							method.instructions.insert(jump, afterJump)
							val falseJump = LabelNode()
							val endJump = LabelNode()
							val list = InsnList().apply {
								add(VarInsnNode(ALOAD, 0))
								add(FieldInsnNode(GETFIELD, "net/minecraft/server/v1_12_R1/EntityLiving", "activeItem", "Lnet/minecraft/server/v1_12_R1/ItemStack;"))
								add(MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/server/v1_12_R1/ItemStack", "getItem", "()Lnet/minecraft/server/v1_12_R1/Item;", false))
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
								add(JumpInsnNode(GOTO, afterJump))
								
								add(falseJump)
								add(endJump)
								//add(ICONST_1)
								//add(IXOR)
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
