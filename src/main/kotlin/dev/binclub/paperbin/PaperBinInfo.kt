package dev.binclub.paperbin

import dev.binclub.paperbin.transformers.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.plugin.Plugin
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.lang.reflect.Proxy
import java.util.logging.Level


/**
 * @author cookiedragon234 23/Apr/2020
 */
object PaperBinInfo {
	val transformers: MutableMap<String, MutableList<(ClassNode) -> Unit>> = hashMapOf()
	val features = arrayOf(
		AntiCrasher,
		AntiDupe,
		AntiNetherRoof,
		BlockTickRateLimiter,
		ChunkLoadingOptimisations,
		FasterGameRuleLookup,
		FoodTpsCompensator,
		MobAiRateLimiter,
		TickCounter,
		VillageRateLimiter
	)
	var enabled = true
	var started = false
	var serverStartTime: Long = 0
	val paperPlugin: Plugin by lazy {
		Proxy.newProxyInstance(this::class.java.classLoader, arrayOf(Plugin::class.java)) { instance, method, args ->
			when (method.name) {
				"isEnabled" -> true
				"getName" -> "PaperBin"
				else -> throw UnsupportedOperationException(method.name)
			}
		} as Plugin
	}
	
	init {
		println("Registering transformers...")
		
		for (feature in features) {
			feature.registerTransformers()
		}
	}
	
	fun registerTransformer(className: String, transformer: (ClassNode) -> Unit) {
		transformers.getOrPut(className, { ArrayList(1) }).add(transformer)
	}
	
	fun onStartup() {
		started = true
		serverStartTime = System.nanoTime()
		
		Bukkit.getCommandMap().register("binstop", object: Command("binstop") {
			override fun execute(sender: CommandSender?, commandLabel: String, args: Array<String?>?): Boolean {
				if (sender?.isOp == true) {
					enabled = false
					sender.sendMessage("Stopped paperbin")
					if (sender !is ConsoleCommandSender) {
						Bukkit.getLogger().log(Level.INFO, "Stopped paperbin")
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
						Bukkit.getLogger().log(Level.INFO, "Started paperbin")
					}
				}
				return true
			}
		})
		
		for (feature in features) {
			feature.postStartup()
		}
	}
	
	// Disable some of the optimisations if the TPS is doing fine
	fun isTpsHigh(): Boolean {
		return false
		//return Bukkit.getTPS()[0] >= 19
	}
	
	var ticks: Int = 0
}

interface PaperFeature {
	@Throws(IllegalStateException::class)
	fun registerTransformers()
	fun register(className: String, transformer: (ClassNode) -> Unit) = PaperBinInfo.registerTransformer(className, transformer)
	fun postStartup() {}
}
