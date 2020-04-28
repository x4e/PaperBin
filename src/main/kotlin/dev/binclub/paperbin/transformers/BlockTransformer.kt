package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.internalName
import net.minecraft.server.v1_12_R1.*
import org.bukkit.Bukkit
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 25/Apr/2020
 */
object BlockTransformer: PaperFeatureTransformer {
	override fun transformClass(classNode: ClassNode) {
		/*for (method in classNode.methods) {
			if (method.name == "a" && method.desc == "(Lnet/minecraft/server/v1_12_R1/World;Lnet/minecraft/server/v1_12_R1/BlockPosition;Lnet/minecraft/server/v1_12_R1/IBlockData;Ljava/util/Random;)V") {
				val list = InsnList().apply {
					add(VarInsnNode(ALOAD, 0))
					add(MethodInsnNode(
						INVOKESTATIC,
						BlockTransformer::class.internalName,
						"shouldTickBlock",
						"(Ljava/lang/Object;)Z"
					))
					val jumpOver = LabelNode()
					add(JumpInsnNode(IFNE, jumpOver))
					add(InsnNode(RETURN))
					add(jumpOver)
				}
				
				method.instructions.insert(list)
				return
			}
		}
		error("Couldnt find target")*/
	}
	
	@JvmStatic
	fun shouldTickBlock(block: Any): Boolean {
		block as Block
		
		if (PaperBinInfo.isTpsHigh() || !PaperBinInfo.enabled) return true
		
		return when (block) {
			block is BlockStationary -> {
				// Stationary lava blocks will every tick try to ignite a nearby block
				// Lets limit this by 30 ticks as this is very unessential
				return PaperBinInfo.ticks % 30 == 0
			}
			block is BlockFlowing -> {
				println("Block flowing")
				return false
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
	
	private val matField by lazy {
		Block::class.java.getDeclaredField("material").also {
			it.isAccessible = true
		}
	}
	
	private fun getMaterialFromBlock(block: Any): Any {
		return matField.get(block)
	}
}
