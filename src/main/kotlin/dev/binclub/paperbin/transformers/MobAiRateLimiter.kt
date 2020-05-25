package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.notify
import dev.binclub.paperbin.utils.wait
import net.minecraft.server.v1_12_R1.*
import org.objectweb.asm.tree.MethodInsnNode
import kotlin.concurrent.thread

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
