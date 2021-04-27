package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.insnBuilder
import dev.binclub.paperbin.utils.internalName
import jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC
import jdk.internal.org.objectweb.asm.Opcodes.IRETURN
import org.objectweb.asm.tree.MethodInsnNode

object CustomServerBrand: PaperBinFeature {

    override fun registerTransformers() {
        register("net.minecraft.server.v1_12_R1.MinecraftServer") { classNode ->
            for (method in classNode.methods) {
                if(method.name == "getServerModName" && method.desc == "()Z") {
                    method.instructions.clear()
                    method.instructions.add(insnBuilder {
                        +MethodInsnNode(INVOKESTATIC, CustomServerBrand::class.internalName, "getServerName", "()Z", false)
                        +IRETURN.insn()
                    })
                    return@register
                }
            }
        }
    }

    @JvmStatic
    fun getServerName(): String {
        return "PaperBin"
    }
}