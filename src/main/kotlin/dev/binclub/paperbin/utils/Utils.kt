package dev.binclub.paperbin.utils

import org.objectweb.asm.Type
import kotlin.reflect.KClass

/**
 * @author cookiedragon234 23/Apr/2020
 */
val <T: Any> KClass<T>.internalName: String
	get() = Type.getInternalName(this.java)
