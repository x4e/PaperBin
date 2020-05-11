package dev.binclub.paperbin

import dev.binclub.paperbin.transformers.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode


/**
 * @author cookiedragon234 23/Apr/2020
 */
object PaperBinInfo {
	val transformers: MutableMap<String, MutableList<(ClassNode) -> Unit>> = hashMapOf()
	var enabled = true
	var started = false
	
	init {
		println("Registering transformers...")
		
		for (transformer in arrayOf(
			AntiDupe,
			BlockTickRateLimiter,
			FasterGameRuleLookup,
			FoodTpsCompensator,
			MobAiRateLimiter,
			TickCounter,
			VillageRateLimiter
		)) {
			transformer.registerTransformers()
		}
	}
	
	fun registerTransformer(className: String, transformer: (ClassNode) -> Unit) {
		transformers.getOrPut(className, { ArrayList(1) }).add(transformer)
	}
	
	fun onStartup() {
		started = true
		
		Bukkit.getCommandMap().register("binstop", object: Command("binstop") {
			override fun execute(sender: CommandSender?, commandLabel: String, args: Array<String?>?): Boolean {
				if (sender?.isOp == true) {
					enabled = false
					sender.sendMessage("Stopped paperbin")
					if (sender !is ConsoleCommandSender) {
						println("Stopped paperbin")
					}
				}
				return true
			}
		})
		
		Bukkit.getCommandMap().register("binstart", object: Command("binstart") {
			override fun execute(sender: CommandSender?, commandLabel: String, args: Array<String?>?): Boolean {
				if (sender?.isOp == true) {
					sender.spigot()
					enabled = true
					sender.sendMessage("Started paperbin")
					if (sender !is ConsoleCommandSender) {
						println("Started paperbin")
					}
				}
				return true
			}
		})
		
		println("Registered event")
	}
	
	// Disable some of the optimisations if the TPS is doing fine
	fun isTpsHigh(): Boolean {
		return false
		//return Bukkit.getTPS()[0] >= 19
	}
	
	var ticks: Int = 0
}

interface PaperFeature {
	fun registerTransformers()
	fun register(className: String, transformer: (ClassNode) -> Unit) = PaperBinInfo.registerTransformer(className, transformer)
}
