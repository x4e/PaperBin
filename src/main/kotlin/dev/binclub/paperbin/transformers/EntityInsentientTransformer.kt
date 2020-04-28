package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.internalName
import net.minecraft.server.v1_12_R1.*
import org.bukkit.Bukkit
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.IllegalStateException

/**
 * @author cookiedragon234 23/Apr/2020
 */
object EntityInsentientTransformer: PaperFeatureTransformer {
	override fun transformClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			if (method.name == "doTick" && method.desc == "()V") {
				for (insn in method.instructions) {
					if (insn is LdcInsnNode) {
						if (insn.cst == "sensing") {
							val load = insn.previous?.previous?.previous
							
							if (load == null) {
								IllegalStateException("Null Load").printStackTrace()
								return
							}
							
							val list = InsnList().apply {
								val out = LabelNode()
								add(VarInsnNode(ALOAD, 0))
								add(MethodInsnNode(INVOKESTATIC, EntityInsentientTransformer::class.internalName, "shouldDoAi", "(Ljava/lang/Object;)Z", false))
								add(JumpInsnNode(IFNE, out))
								add(InsnNode(RETURN))
								add(out)
							}
							
							method.instructions.insertBefore(load, list)
							return
						}
					}
				}
			}
		}
		error("Couldnt find target")
	}
	
	@JvmStatic
	fun shouldDoAi(entity: Any): Boolean {
		entity as EntityInsentient
		
		if (PaperBinInfo.isTpsHigh() || !PaperBinInfo.enabled) return true
		
		return when (entity) {
			is EntityBat -> false
			is EntityRabbit -> false
			is EntityPolarBear -> false
			is EntityArmorStand -> false
			is EntitySnowman -> PaperBinInfo.ticks % 25 == 0
			is EntityEndermite -> PaperBinInfo.ticks % 25 == 0
			is EntityParrot -> PaperBinInfo.ticks % 25 == 0
			is EntityPigZombie -> PaperBinInfo.ticks % 25 == 0
			is EntityVillager -> (PaperBinInfo.ticks % 15 == 0).also {
				EntityVillagerTransformer.handleInsentientVillagerUpdate(it, entity)
			}
			is EntityMonster -> PaperBinInfo.ticks % 15 == 0
			else -> PaperBinInfo.ticks % 5 == 0
		} && entity.world.findNearbyPlayer(entity, 40.0) != null // Only calculate if there are nearby players
	}
}
