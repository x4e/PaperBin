package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.internalName
import net.minecraft.server.v1_12_R1.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 11/May/2020
 */
object BlockTickRateLimiter: PaperBinFeature {
	@JvmStatic
	fun shouldTickBlock(block: Any): Boolean {
		block as Block
		
		if (!PaperBinConfig.blockRateLimit) return true
		
		return when (block) {
			block is BlockStationary -> {
				// Stationary lava blocks will every tick try to ignite a nearby block
				// Lets limit this by 30 ticks as this is very unessential
				return PaperBinInfo.ticks % 30 == 0
			}
			block is BlockFlowing -> {
				return PaperBinInfo.ticks % 30 == 0
			}
			block is BlockMagma -> {
				// Magma blocks emit smoke push air above them
				// Honestly why
				// Lets reduce this by 1/30th
				return PaperBinInfo.ticks % 30 == 0
			}
			block is BlockLeaves -> {
				// Lower rate at which leaf decay calculation is performed
				return PaperBinInfo.ticks % 30 == 0
			}
			block is BlockGrass -> {
				// Lower rate at which grass spreads
				return PaperBinInfo.ticks % 20 == 0
			}
			block is BlockLongGrass -> {
				return PaperBinInfo.ticks % 10 == 0
			}
			block is BlockSoil -> {
				// Soil blocks calculate their moisture or something continuously lmao
				return PaperBinInfo.ticks % 5 == 0
			}
			block is BlockFire -> {
				// Lower rate at which fire spreads
				return PaperBinInfo.ticks % 15 == 0
			}
			else -> true
		}
	}
	
	override fun registerTransformers() {
		if (!PaperBinConfig.blockRateLimit) return
		
		register("net.minecraft.server.v1_12_R1.BlockFlowing") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "b" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/IBlockData;Ljava/util/Random;)V") {
					val list = InsnList().apply {
						add(VarInsnNode(ALOAD, 0))
						add(
							MethodInsnNode(
							INVOKESTATIC,
							BlockTickRateLimiter::class.internalName,
							"shouldTickBlock",
							"(Ljava/lang/Object;)Z",
							false
						)
						)
						val jumpOver = LabelNode()
						add(JumpInsnNode(IFNE, jumpOver))
						add(InsnNode(RETURN))
						add(jumpOver)
					}
					method.instructions.insert(list)
					
					return@register
				}
			}
			error("Couldnt find target")
		}
		register("net.minecraft.server.v1_12_R1.BlockLeaves") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "b" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/IBlockData;Ljava/util/Random;)V") {
					val list = InsnList().apply {
						add(VarInsnNode(ALOAD, 0))
						add(
							MethodInsnNode(
								INVOKESTATIC,
								BlockTickRateLimiter::class.internalName,
								"shouldTickBlock",
								"(Ljava/lang/Object;)Z",
								false
							)
						)
						val jumpOver = LabelNode()
						add(JumpInsnNode(IFNE, jumpOver))
						add(InsnNode(RETURN))
						add(jumpOver)
					}
					method.instructions.insert(list)
					
					return@register
				}
			}
			error("Couldnt find target")
		}
		register("net.minecraft.server.v1_12_R1.BlockMagma") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "b" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/IBlockData;Ljava/util/Random;)V") {
					val list = InsnList().apply {
						add(VarInsnNode(ALOAD, 0))
						add(
							MethodInsnNode(
								INVOKESTATIC,
								BlockTickRateLimiter::class.internalName,
								"shouldTickBlock",
								"(Ljava/lang/Object;)Z",
								false
							)
						)
						val jumpOver = LabelNode()
						add(JumpInsnNode(IFNE, jumpOver))
						add(InsnNode(RETURN))
						add(jumpOver)
					}
					method.instructions.insert(list)
					
					return@register
				}
			}
			error("Couldnt find target")
		}
		register("net.minecraft.server.v1_12_R1.BlockStationary") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "b" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/IBlockData;Ljava/util/Random;)V") {
					val list = InsnList().apply {
						add(VarInsnNode(ALOAD, 0))
						add(MethodInsnNode(
							INVOKESTATIC,
							BlockTickRateLimiter::class.internalName,
							"shouldTickBlock",
							"(Ljava/lang/Object;)Z",
							false
						))
						val jumpOver = LabelNode()
						add(JumpInsnNode(IFNE, jumpOver))
						add(InsnNode(RETURN))
						add(jumpOver)
					}
					method.instructions.insert(list)
					
					return@register
				}
			}
			error("Couldnt find target")
		}
		//register("net.minecraft.server.v1_12_R1.Block") { classNode -> }
	}
}
