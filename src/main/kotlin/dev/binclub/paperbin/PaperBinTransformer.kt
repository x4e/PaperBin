package dev.binclub.paperbin

import dev.binclub.paperbin.PaperBinInfo.logger
import dev.binclub.paperbin.native.PaperBinClassTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.lang.reflect.Modifier
import java.security.ProtectionDomain
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.logging.Level
import kotlin.concurrent.thread

/**
 * @author cookiedragon234 12/Apr/2020
 */
object PaperBinTransformer: PaperBinClassTransformer {
	private val jar: JarOutputStream? =
		if (PaperBinConfig.debug) {
			val file = File("paperbin_patched.jar")
			if (file.exists()) file.delete()
			JarOutputStream(file.outputStream()).also {
				Runtime.getRuntime().addShutdownHook(thread(start = false, isDaemon = false) {
					it.close()
				})
			}
		} else null
	
	val transforming = HashMap<String, ClassNode>()
	
	private val ignores = arrayOf(
		"java/"
	)
	
	override fun onClassLoad(
		clazz: Class<*>?,
		loader: ClassLoader?,
		className: String?,
		classfileBuffer: ByteArray
	): ByteArray? {
		if (className == null || ignores.any { className.startsWith(it, true) }) {
			return null
		}
		
		val internalName = className.replace('.', '/')
		try {
			PaperBinInfo.transformers[internalName]?.let { transformers ->
				if (transformers.isNotEmpty()) {
					PaperBinInfo.usedTransformers += internalName
					logger.log(Level.INFO, "Transforming [$internalName] (${transformers.size})...")
					
					val classNode = ClassNode()
					ClassReader(classfileBuffer).accept(classNode, 0)
					transforming[className.replace('/', '.')] = classNode
					
					transformers.forEach {
						try {
							it.first.invoke(classNode)
						} catch (t: Throwable) {
							logger.log(Level.SEVERE, "Error transforming [$internalName] with transformer [$it]", t)
							handleShutdown(t)
						}
					}
					
					val writer = NoLoadClassWriter(ClassWriter.COMPUTE_FRAMES)
					try {
						classNode.accept(writer)
						
						return writer.toByteArray().also {
							if (jar != null) {
								jar.putNextEntry(JarEntry("$internalName.class"))
								jar.write(it)
								jar.closeEntry()
							}
						}
					} catch (t: Throwable) {
						logger.log(Level.SEVERE, "Error transforming [$internalName]", t)
						
						try {
							classNode.methods?.forEach {
								// hacky
								it.maxStack += 10
								it.maxLocals += 5
							}
							val writer = NoLoadClassWriter(ClassWriter.COMPUTE_MAXS)
							classNode.accept(writer)
							classNode.accept(CheckClassAdapter(null, true))
						} catch (t: Throwable) {
							logger.log(Level.SEVERE, "", t)
						}
						
						try {
							if (jar != null) {
								val writer = NoLoadClassWriter(0)
								
								classNode.accept(writer)
								
								writer.toByteArray().also {
									jar.putNextEntry(JarEntry("$internalName.class"))
									jar.write(it)
									jar.closeEntry()
								}
							}
						} catch (t: Throwable) {
						}
						
						handleShutdown(t)
					}
				}
			}
			
			return null
		} finally {
			transforming -= internalName
		}
	}
	
	override fun onClassPrepare(clazz: Class<*>) {
		val internalName = Type.getInternalName(clazz)
		PaperBinInfo.transformers[internalName]?.let { transformers ->
			if (transformers.isNotEmpty()) {
				transformers.forEach {
					try {
						it.second?.invoke(clazz)
					} catch (t: Throwable) {
						logger.log(Level.SEVERE, "Error running class prepare event for [$internalName] with listener [$it]", t)
						handleShutdown(t)
					}
				}
			}
		}
	}
	
	fun findClassType(name: String): ClassType {
		transforming[name]?.let {
			return AsmClassType(it)
		}
		return JvmClassType(Class.forName(name, false, this::class.java.classLoader))
	}
	
	interface ClassType {
		val name: String
		val parent: ClassType?
		val interfaces: List<ClassType>
		val isInterface: Boolean
		val supers: Set<String>
	}
	
	class JvmClassType(cn: Class<*>): ClassType {
		override val name: String = cn.name
		override val parent: ClassType? = cn.superclass?.let { JvmClassType(it) }
		override val interfaces: List<ClassType> by lazy { cn.interfaces.map { JvmClassType(it) } }
		override val isInterface: Boolean = cn.isInterface
		override val supers: Set<String> by lazy { supersOf(this, HashSet()) }
	}
	
	class AsmClassType(cn: ClassNode): ClassType {
		override val name: String = cn.name.replace('/', '.')
		override val parent: ClassType? = if (name == "java.lang.Object") null else findClassType(
			cn.superName.replace(
				'/',
				'.'
			)
		)
		override val interfaces: List<ClassType> by lazy { cn.interfaces?.map { findClassType(it.replace('/', '.')) } ?: emptyList() }
		override val isInterface: Boolean = Modifier.isInterface(cn.access)
		override val supers: Set<String> by lazy { supersOf(this, HashSet()) }
	}
	
	fun supersOf(ct: ClassType, collect: MutableSet<String>): MutableSet<String> = collect.also {
		ct.parent?.let {
			collect.add(it.name)
			supersOf(it, collect)
		}
		ct.interfaces.forEach {
			collect.add(it.name)
			supersOf(it, collect)
		}
	}
	
	class NoLoadClassWriter(flags: Int) : ClassWriter(flags) {
		
		override fun getCommonSuperClass(type1: String, type2: String): String {
			if (type1 == type2) return type1
			
			if (type1 == "net/minecraft/server/v1_12_R1/EntityHuman" && type2 == "net/minecraft/server/v1_12_R1/EntityLiving") {
				return type2
			}
			if (type2 == "net/minecraft/server/v1_12_R1/EntityHuman" && type1 == "net/minecraft/server/v1_12_R1/EntityLiving") {
				return type1
			}
			
			try {
				val cn1 = findClassType(type1.replace('/', '.'))
				val cn2 = findClassType(type2.replace('/', '.'))
				
				return common(cn1, cn2)
			} catch (t: Throwable) {
				t.printStackTrace()
				return "java/lang/Object"
			}
		}
		
		private val PRIMITIVE_DESCRIPTORS = charArrayOf('V', 'Z', 'C', 'B', 'S', 'I', 'F', 'J', 'D')
		
		/**
		 * Determines if the class or interface represented by this object is either the same as, or is a superclass or
		 * superinterface of, the class or interface represented by the specified parameter. It returns true if so;
		 * otherwise it returns false. If this object represents a primitive type, this method returns true if the
		 * specified parameter is exactly this object; otherwise it returns false
		 */
		private fun ClassType.isAssignableFrom(cn: ClassType): Boolean {
			if (cn.name == name) {
				return true
			}
			if (name.length == 1 && PRIMITIVE_DESCRIPTORS.contains(name[0])) {
				// primitives must exactly match and first statement asserted they dont
				return false
			}
			return this.name in cn.supers
		}
		
		private fun common(c1: ClassType, c2: ClassType): String {
			return if (c1.isAssignableFrom(c2)) {
				c1.name.replace('.', '/')
			} else if (c2.isAssignableFrom(c1)) {
				c2.name.replace('.', '/')
			} else if (!c1.isInterface && !c2.isInterface) {
				var c1 = c1
				do {
					c1 = c1.parent ?: break
				} while (!c1.isAssignableFrom(c2))
				c1.name.replace('.', '/')
			} else {
				"java/lang/Object"
			}
		}
	}
}
