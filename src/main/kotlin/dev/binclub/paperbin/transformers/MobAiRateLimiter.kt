package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.internalName
import net.minecraft.server.v1_12_R1.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.IllegalStateException

/**
 * @author cookiedragon234 11/May/2020
 */
object MobAiRateLimiter: PaperFeature {
	override fun registerTransformers() {
		if (!PaperBinConfig.mobAiRateLimit) return
		
		register("net.minecraft.server.v1_12_R1.PathfinderGoalSelector") { classNode ->
			for (field in classNode.fields) {
				if (field.name == "f" && field.desc == "I") {
					field.value = 30 // Update goals 1/10th of the time
					return@register
				}
			}
			error("Couldnt find target")
		}
	}
}
