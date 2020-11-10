package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.native.NativeAccessor
import dev.binclub.paperbin.utils.insnBuilder
import net.minecraft.server.v1_12_R1.Block
import net.minecraft.server.v1_12_R1.BlockPosition
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM6
import org.objectweb.asm.tree.MethodNode

/**
 * @author cookiedragon234 17/Aug/2020
 */
object AntiPhysicsCrash: PaperBinFeature {
	override fun registerTransformers() {
		if (!PaperBinConfig.antiPhysicsCrash) return
		
		PaperBinInfo.registerTransformer("net/minecraft/server/v1_12_R1/World", {}) { cl ->
			val method = cl.getDeclaredMethod("applyPhysics", BlockPosition::class.java, Block::class.java, java.lang.Boolean.TYPE)
				?: error("Couldn't find physics method")
			NativeAccessor.registerAntiPhysicsCrash(method, PaperBinConfig.physicsMaxStackSize)
		}
	}
}
