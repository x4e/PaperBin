@file:Suppress("NOTHING_TO_INLINE", "unused", "SpellCheckingInspection")

package dev.binclub.paperbin.utils

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 18/May/2020
 */
fun insnBuilder(application: InsnBuilder.() -> Unit): InsnList {
	return InsnBuilder().apply(application).list
}

@Suppress("FunctionName")
class InsnBuilder {
	val list = InsnList()
	
	inline operator fun InsnList.unaryPlus() = list.add(this)
	inline operator fun AbstractInsnNode.unaryPlus() = list.add(this)
	inline fun Int.insn() = InsnNode(this)
	
	inline fun insn(opcode: Int) = +InsnNode(opcode)
	inline fun aconst_null() = insn(ACONST_NULL)
	inline fun pop() = insn(POP)
	inline fun ineg() = insn(INEG)
	inline fun isub() = insn(ISUB)
	inline fun iadd() = insn(IADD)
	inline fun imul() = insn(IMUL)
	inline fun ior() = insn(IOR)
	inline fun iand() = insn(IAND)
	inline fun ixor() = insn(IXOR)
	inline fun swap() = insn(SWAP)
	inline fun dup() = insn(DUP)
	inline fun dup_x1() = insn(DUP_X1)
	inline fun dup_x2() = insn(DUP_X2)
	inline fun dup2() = insn(DUP2)
	inline fun iconst_1() = insn(ICONST_1)
	inline fun iconst_m1() = insn(ICONST_M1)
	inline fun ifeq(labelNode: LabelNode) = +JumpInsnNode(IFEQ, labelNode)
	inline fun ifnull(labelNode: LabelNode) = +JumpInsnNode(IFNULL, labelNode)
	inline fun aload(`var`: Int) = +VarInsnNode(ALOAD, `var`)
	inline fun invokestatic(owner: String, name: String, desc: String, `interface`: Boolean = false)
		= +MethodInsnNode(INVOKESTATIC, owner, name, desc, `interface`)
	inline fun invokevirtual(owner: String, name: String, desc: String, `interface`: Boolean = false)
		= +MethodInsnNode(INVOKEVIRTUAL, owner, name, desc, `interface`)
}
