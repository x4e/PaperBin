package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.insnBuilder
import net.minecraft.server.v1_12_R1.*
import org.objectweb.asm.Opcodes.RETURN

/**
 * Anti illegal item
 *
 * Prevents obtaining creative only items, unnatural stack counts, and unnatural enchantment levels
 *
 * For peformance, this will only run on itemstack deserialization
 *
 * This means the following events will trigger item stack sanitization:
 * - Player/Entity log on (inventory is sanitized)
 * - Chunk load (tile entity inventories are sanitized)
 *
 * This is much faster and more reliable than other alternatives
 *
 * @author cookiedragon234 06/Aug/2020
 */
object AntiIllegalItem: PaperBinFeature {
	// Used to prevent class loading
	private class Lazy {
		val AIR = Item.getItemOf(Blocks.AIR)
		
		val nonSurvivalItems: Set<Item> = hashSetOf(
			Item.getItemOf(Blocks.BEDROCK),
			Item.getItemOf(Blocks.PORTAL),
			Item.getItemOf(Blocks.END_PORTAL),
			Item.getItemOf(Blocks.END_PORTAL_FRAME),
			Item.getItemOf(Blocks.COMMAND_BLOCK),
			Item.getItemOf(Blocks.STRUCTURE_BLOCK),
			Item.getItemOf(Blocks.MOB_SPAWNER),
			Item.getItemOf(Blocks.BARRIER),
			Items.db, // Sorry riga
			Items.COMMAND_BLOCK_MINECART,
			Items.SPAWN_EGG
			
			// TODO: 1.13+ has item "debug_stick"
		)
	}
	
	private val lazy: Lazy by lazy { Lazy() }
	
	@JvmStatic
	fun validateItemStack(itemStack: ItemStack) {
		if (itemStack == ItemStack.a) return
		
		if (itemStack.item in lazy.nonSurvivalItems) {
			itemStack.item = lazy.AIR
			itemStack.count = 0
			return
		}
		
		var count = itemStack.count
		count = count.coerceAtLeast(0)
		count = count.coerceAtMost(itemStack.maxStackSize)
		itemStack.count = count
		
		itemStack.tag?.let { tag ->
			tag.getList("ench", 10)?.let { enchantments ->
				for (nbt in enchantments) {
					val enchantment = Enchantment.c(nbt.getShort("id").toInt()) ?: continue
					if (nbt.hasKey("lvl")) {
						var level = nbt.getShort("lvl")
						level = level.coerceAtLeast(0)
						level = level.coerceAtMost(enchantment.maxLevel.toShort())
						nbt.setShort("lvl", level)
					}
				}
			}
			
			val unbreakable = if (tag.hasKey("Unbreakable")) tag.getBoolean("Unbreakable") else false
			
			if (!unbreakable) {
				var damage = itemStack.damage
				damage = damage.coerceAtLeast(0)
				itemStack.damage = damage
			}
		}
	}
	
	operator fun NBTTagList.iterator(): Iterator<NBTTagCompound> = object: Iterator<NBTTagCompound> {
		var index = 0
		val list = this@iterator
		
		override fun hasNext(): Boolean = index < list.size()
		
		override fun next(): NBTTagCompound {
			val out = list.get(index)
			index += 1
			return out
		}
	}
	
	override fun registerTransformers() {
		if (!PaperBinConfig.antiIllegalItem) return
		
		register("net.minecraft.server.v1_12_R1.ItemStack") { cn ->
			cn.methods.forEach { mn ->
				if (mn.name == "<init>" && mn.desc == "(Lnet/minecraft/server/v1_12_R1/NBTTagCompound;)V") {
					for (insn in mn.instructions) {
						if (insn.opcode == RETURN) {
							val insert = insnBuilder {
								aload(0)
								invokestatic(
									"dev/binclub/paperbin/transformers/AntiIllegalItem",
									"validateItemStack",
									"(Lnet/minecraft/server/v1_12_R1/ItemStack;)V"
								)
							}
							mn.instructions.insertBefore(insn, insert)
							return@register
						}
					}
				}
			}
			error("Couldn't find target")
		}
	}
}
