package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperFeature

/**
 * @author cookiedragon234 12/May/2020
 */
object AntiNetherRoof: PaperFeature {
	override fun registerTransformers() {
		if (!PaperBinConfig.antiNetherRoof) return
	}
}
