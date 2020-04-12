package dev.binclub.paperbin

import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

/**
 * @author cookiedragon234 12/Apr/2020
 */
class PaperBinTransformer: ClassFileTransformer {
	init {
		println("Transformer init")
	}
	
	override fun transform(
		loader: ClassLoader?,
		className: String?,
		classBeingRedefined: Class<*>?,
		protectionDomain: ProtectionDomain?,
		classfileBuffer: ByteArray?
	): ByteArray? {
		println("Transforming $className")
		
		return classfileBuffer
	}
}
