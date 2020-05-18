package dev.binclub.paperbin.utils

import dev.binclub.paperbin.PaperBinInfo
import org.objectweb.asm.Type
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.io.PrintStream
import kotlin.reflect.KClass

/**
 * @author cookiedragon234 23/Apr/2020
 */
val <T: Any> KClass<T>.internalName: String
	get() = Type.getInternalName(this.java)

fun InsnList.add(opcode: Int) = add(InsnNode(opcode))

fun ldcInt(int: Int): AbstractInsnNode {
	return if (int == -1) {
		InsnNode(ICONST_M1)
	} else if (int == 0) {
		InsnNode(ICONST_0)
	} else if (int == 1) {
		InsnNode(ICONST_1)
	} else if (int == 2) {
		InsnNode(ICONST_2)
	} else if (int == 3) {
		InsnNode(ICONST_3)
	} else if (int == 4) {
		InsnNode(ICONST_4)
	} else if (int == 5) {
		InsnNode(ICONST_5)
	} else if (int >= -128 && int <= 127) {
		IntInsnNode(BIPUSH, int)
	} else if (int >= -32768 && int <= 32767) {
		IntInsnNode(SIPUSH, int)
	} else {
		LdcInsnNode(int)
	}
}

fun printlnAsm(): InsnList {
	return InsnList().apply {
		add(FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
		add(SWAP)
		add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/Object;)V", false))
	}
}

fun printlnAsm(text: String): InsnList {
	return InsnList().also {
		it.add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "Ljava/io/PrintStream;"))
		it.add(LdcInsnNode(text))
		it.add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/String;)V", false))
	}
}

fun printlnIntAsm(): InsnList {
	return InsnList().also {
		it.add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "Ljava/io/PrintStream;"))
		it.add(InsnNode(SWAP))
		it.add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(I)V", false))
	}
}
