package dev.binclub.paperbin.utils

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode

/**
 * @author cookiedragon234 18/May/2020
 */
fun insnBuilder(application: InsnBuilder.() -> Unit): InsnList {
	return InsnBuilder().apply(application).list
}

class InsnBuilder {
	internal val list = InsnList()
	
	operator fun AbstractInsnNode.unaryPlus() = list.add(this)
	fun Int.insn() = InsnNode(this)
}
