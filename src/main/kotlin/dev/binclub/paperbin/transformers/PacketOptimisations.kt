package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.add
import net.minecraft.server.v1_12_R1.MCUtil
import net.minecraft.server.v1_12_R1.NetworkManager
import org.objectweb.asm.Opcodes.ICONST_1
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode
import java.lang.invoke.MethodHandles

/**
 * @author cookiedragon234 13/May/2020
 */
object PacketOptimisations: PaperFeature {
	override fun registerTransformers() {
	}
}
