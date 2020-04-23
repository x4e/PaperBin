package dev.binclub.paperbin.transformers

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 23/Apr/2020
 */
interface PaperFeatureTransformer {
	fun transformClass(classNode: ClassNode)
}
