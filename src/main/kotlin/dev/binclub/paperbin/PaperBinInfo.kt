package dev.binclub.paperbin

import dev.binclub.paperbin.transformers.*
import dev.binclub.paperbin.transformers.asyncai.AsyncMobAi
import dev.binclub.paperbin.utils.NopSet
import dev.binclub.paperbin.utils.StdOutHandler
import dev.binclub.paperbin.utils.checkForUpdate
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
import kotlin.concurrent.thread


/**
 * @author cookiedragon234 23/Apr/2020
 */
object PaperBinInfo {
	val version = 1.82f
	@JvmStatic
	val logger by lazy {
		Logger.getLogger("PaperBin").also {
			for (handler in it.handlers) {
				it.removeHandler(handler)
			}
			it.useParentHandlers = false
			it.addHandler(StdOutHandler().apply {
				formatter = object: Formatter() {
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
	var crashed = false
	val transformers: MutableMap<String, MutableList<Pair<(ClassNode) -> Unit, ((Class<*>) -> Unit)?>>> = hashMapOf()
	val usedTransformers: MutableSet<String> =
		(if (PaperBinConfig.debug) hashSetOf<String>() else NopSet<String>()).also { usedTransformers ->
			Runtime.getRuntime().addShutdownHook(thread(start = false, isDaemon = false) {
				if (PaperBinConfig.debug && !crashed) {
					if (transformers.isEmpty()) {
						logger.info("All paperbin transformers consumed")
					}
					for (transformer in transformers.keys) {
						if (transformer !in usedTransformers) {
							logger.warning("Transformer [$transformer] was never used")
						}
					}
				}
			})
		}
	val features = arrayOf(
		AntiChunkBan,
		AntiCrasher,
		AntiDupe,
		AntiElytraFly,
		AntiEntityDesync,
		AntiGrief,
		AntiIllegalItem,
		AntiNetherRoof,
		AntiNewChunks,
		AntiPhysicsCrash,
		AntiPortalGodmode,
		AntiUnicodeChat,
		AsyncMobAi,
		BlockTickRateLimiter,
		ChunkLoadingOptimisations,
		CustomNbtEvents,
		ElytraLowerRenderDistance,
		FasterGameRuleLookup,
		TpsCompensation,
		LightUpdateRateLimiter,
		MobAiRateLimiter,
		OptimisedEveryoneSleeping,
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
	
	fun registerTransformer(className: String, transformer: (ClassNode) -> Unit, postTransformer: ((Class<*>) -> Unit)? = null) {
		transformers.getOrPut(className.replace('.', '/'), { ArrayList(1) }).add(transformer to postTransformer)
	}
	
	fun onStartup() {
		started = true
		serverStartTime = System.nanoTime()
		
		checkForUpdate()
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

interface PaperBinFeature {
	val logger: Logger
		get() = PaperBinInfo.logger
	
	@Throws(IllegalStateException::class)
	fun registerTransformers()
	fun register(className: String, transformer: (ClassNode) -> Unit) = PaperBinInfo.registerTransformer(className, transformer)
	fun postStartup() {}
}
