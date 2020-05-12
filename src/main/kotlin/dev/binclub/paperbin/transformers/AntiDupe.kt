package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.add
import dev.binclub.paperbin.utils.printlnAsm
import net.minecraft.server.v1_12_R1.BlockShulkerBox
import net.minecraft.server.v1_12_R1.Entity
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld
import org.bukkit.craftbukkit.v1_12_R1.event.CraftEventFactory
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 11/May/2020
 */
object AntiDupe: PaperFeature {
	override fun registerTransformers() {
		// If an item is dropped then we duplicate it and empty the original stack
		// This should prevent some duplication glitches, e.g. 11/11
		register("org.bukkit.craftbukkit.v1_12_R1.event.CraftEventFactory") { classNode ->
			var i = 0
			for (method in classNode.methods) {
				for (insn in method.instructions) {
					if (
						insn is MethodInsnNode
						&&
						(insn.owner == "org/bukkit/craftbukkit/v1_12_R1/CraftWorld" || insn.owner == "org/bukkit/World")
						&&
						insn.name == "dropItemNaturally"
						&&
						insn.desc == "(Lorg/bukkit/Location;Lorg/bukkit/inventory/ItemStack;)Lorg/bukkit/entity/Item;"
					) {
						val before = InsnNode(DUP_X2)
						val after = InsnList().apply {
							val jmpEnd = LabelNode()
							val end = LabelNode()
							add(SWAP)
							add(DUP)
							add(TypeInsnNode(INSTANCEOF, "org/bukkit/craftbukkit/v1_12_R1/inventory/CraftItemStack"))
							add(JumpInsnNode(IFEQ, jmpEnd))
							add(ICONST_0)
							add(MethodInsnNode(
								INVOKEVIRTUAL,
								"org/bukkit/inventory/ItemStack",
								"setAmount",
								"(I)V",
								false
							))
							add(JumpInsnNode(GOTO, end))
							add(jmpEnd)
							add(POP)
							add(end)
						}
						method.instructions.insertBefore(insn, before)
						method.instructions.insert(insn, after)
						i += 1
					}
				}
			}
			
			if (i < 2) error("Couldnt find target $i")
		}
		register("net.minecraft.server.v1_12_R1.Entity") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "bf" && method.desc == "()Z") {
					val list = InsnList().apply {
						add(VarInsnNode(ALOAD, 0))
						add(MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/server/v1_12_R1/Entity", "isAlive", "()Z", false))
						add(IRETURN) // Only allow alive entities to teleport
					}
					method.instructions.insert(list)
				}
				
				if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/ItemStack;F)Lnet/minecraft/server/v1_12_R1/EntityItem;") {
					for (insn in method.instructions) {
						if (insn is MethodInsnNode && insn.name == "asBukkitCopy") {
							insn.name = "asCraftMirror"
							insn.desc = "(Lnet/minecraft/server/v1_12_R1/ItemStack;)Lorg/bukkit/craftbukkit/v1_12_R1/inventory/CraftItemStack;"
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
}
