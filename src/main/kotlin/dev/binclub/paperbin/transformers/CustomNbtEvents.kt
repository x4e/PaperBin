package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.add
import net.minecraft.server.v1_12_R1.Entity
import net.minecraft.server.v1_12_R1.NBTTagCompound
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * @author cookiedragon234 16/May/2020
 */
object CustomNbtEvents: PaperBinFeature {
	@JvmStatic
	fun event(entity: Any, nbt: Any, read: Boolean) {
		entity as Entity
		nbt as NBTTagCompound
		
		if (read) {
			ReadEntityNbtEvent(entity, nbt).callEvent()
		} else {
			SaveEntityNbtEvent(entity, nbt).callEvent()
		}
	}
	
	override fun registerTransformers() {
		return
		register("net.minecraft.server.v1_12_R1.Entity") { classNode ->
			for (method in classNode.methods) {
				if (method.name == "f" && method.desc == "(Lnet/minecraft/server/v1_12_R1/NBTTagCompound;)V") {
					val list = InsnList().apply {
						add(VarInsnNode(ALOAD, 0))
						add(VarInsnNode(ALOAD, 1))
						add(ICONST_1)
						add(MethodInsnNode(INVOKESTATIC, "dev/binclub/paperbin/transformers/CustomNbtEvents", "event", "(Ljava/lang/Object;Ljava/lang/Object;Z)V", false))
					}
					method.instructions.insert(list)
				} else if (method.name == "save" && method.desc == "(Lnet/minecraft/server/v1_12_R1/NBTTagCompound;)Lnet/minecraft/server/v1_12_R1/NBTTagCompound;") {
					val list = InsnList().apply {
						add(VarInsnNode(ALOAD, 0))
						add(VarInsnNode(ALOAD, 1))
						add(ICONST_0)
						add(MethodInsnNode(INVOKESTATIC, "dev/binclub/paperbin/transformers/CustomNbtEvents", "event", "(Ljava/lang/Object;Ljava/lang/Object;Z)V", false))
					}
					method.instructions.insert(list)
				}
			}
		}
	}
}

data class ReadEntityNbtEvent(val entity: Entity, val nbt: NBTTagCompound): Event() {
	private val handlers = HandlerList()
	override fun getHandlers(): HandlerList = handlers
}

data class SaveEntityNbtEvent(val entity: Entity, val nbt: NBTTagCompound): Event() {
	private val handlers = HandlerList()
	override fun getHandlers(): HandlerList = handlers
}
