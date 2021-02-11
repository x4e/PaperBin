package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.insnBuilder
import dev.binclub.paperbin.utils.printlnAsm
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * @author cookiedragon234 02/Jul/2020
 */
object AntiEntityDesync: PaperBinFeature {
	override fun registerTransformers() {
		return
		register("net.minecraft.server.v1_12_R1.Entity") { classNode ->
			var done = 0
			for (method in classNode.methods) {
				if (method.name == "aE" && method.desc == "()V") {
					method.instructions.insert(insnBuilder {
						printlnAsm("Update Ridden")
						done += 1
					})
				}
				
				if (method.name == "k" && method.desc == "(Lnet/minecraft/server/v1_12_R1/Entity;)V") {
					method.instructions.insert(insnBuilder {
						printlnAsm("Tick passenger")
						done += 1
					})
				}
				
				if (method.name == "postTick" && method.desc == "()V") {
					method.instructions.insert(insnBuilder {
						+VarInsnNode(ALOAD, 0)
						+MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/server/v1_12_R1/Entity", "aE", "()V", false)
						done += 1
					})
				}
			}
			if (done < 3) {
				error("Couldnt find target $done")
			}
		}
	}
}
