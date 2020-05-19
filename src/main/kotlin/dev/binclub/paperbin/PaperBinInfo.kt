package dev.binclub.paperbin

import dev.binclub.paperbin.transformers.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import org.objectweb.asm.tree.ClassNode
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Proxy
import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger


/**
 * @author cookiedragon234 23/Apr/2020
 */
object PaperBinInfo {
	val version = 1.56f
	val logger by lazy {
		Logger.getLogger("PaperBin").also {
			for (handler in it.handlers) {
				it.removeHandler(handler)
			}
			it.useParentHandlers = false
			it.addHandler(ConsoleHandler().also {
				it.formatter = object: Formatter() {
					override fun format(record: LogRecord): String {
						val builder = StringBuilder()
						val ex = record.thrown
						builder.append("\r[paperbin ")
						builder.append(record.level.localizedName.toUpperCase())
						builder.append("] ")
						builder.append(formatMessage(record))
						builder.append('\n')
						if (ex != null) {
							val writer = StringWriter()
							ex.printStackTrace(PrintWriter(writer))
							builder.append(writer)
						}
						return builder.toString()
					}
				}
			})
		}
	}
	
	var started = false
	val transformers: MutableMap<String, MutableList<(ClassNode) -> Unit>> = hashMapOf()
	val features = arrayOf(
		AntiChunkBan,
		AntiCrasher,
		AntiDupe,
		AntiGrief,
		AntiNetherRoof,
		AntiNewChunks,
		AntiPortalGodmode,
		AntiUnicodeChat,
		BlockTickRateLimiter,
		ChunkLoadingOptimisations,
		CustomNbtEvents,
		FasterGameRuleLookup,
		FoodTpsCompensator,
		MobAiRateLimiter,
		OptimisedEveryoneSleeping,
		OptimisedGetEntityPassengers,
		PacketOptimisations,
		TickCounter,
		VillageRateLimiter
	)
	var serverStartTime: Long = 0
	val paperPlugin: Plugin by lazy {
		Proxy.newProxyInstance(this::class.java.classLoader, arrayOf(Plugin::class.java)) { instance, method, args ->
			when (method.name) {
				"isEnabled" -> true
				"getName" -> "PaperBin"
				"equals" -> args[0] == instance
				else -> throw UnsupportedOperationException(method.name)
			}
		} as Plugin
	}
	
	init {
		logger.info("Registering transformers...")
		
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
		
		if (PaperBinConfig.debug) {
			logger.warning("--------------------------------------------------")
			logger.warning("WARNING: PaperBin has been started with DEBUG mode")
			logger.warning("This WILL impact performance!")
			logger.warning("--------------------------------------------------")
		}
		
		Bukkit.getCommandMap().register("binreload", object: Command("binreload") {
			override fun execute(sender: CommandSender?, commandLabel: String, args: Array<String?>?): Boolean {
				if (sender?.isOp == true) {
					if (PaperBinConfig.load()) {
						sender.sendMessage("§6Reloaded PaperBin config")
					} else {
						sender.sendMessage("§cFailed to reload PaperBin config")
					}
					return true
				}
				return false
			}
		})
		
		Bukkit.getCommandMap().register("binsave", object: Command("binsave") {
			override fun execute(sender: CommandSender?, commandLabel: String, args: Array<String?>?): Boolean {
				if (sender?.isOp == true) {
					if (PaperBinConfig.save()) {
						sender.sendMessage("§6Saved PaperBin config")
					} else {
						sender.sendMessage("§cFailed to save PaperBin config")
					}
					return true
				}
				return false
			}
		})
		
		Bukkit.getCommandMap().register("paperbin", object: Command("paperbin") {
			override fun execute(sender: CommandSender?, commandLabel: String, args: Array<String?>?): Boolean {
				sender?.sendMessage("§6This server is running PaperBin $version")
				return true
			}
		})
		
		for (feature in features) {
			feature.postStartup()
		}
	}
	
	var ticks: Int = 0
}

interface PaperFeature {
	@Throws(IllegalStateException::class)
	fun registerTransformers()
	fun register(className: String, transformer: (ClassNode) -> Unit) = PaperBinInfo.registerTransformer(className, transformer)
	fun postStartup() {}
}
