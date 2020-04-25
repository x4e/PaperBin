package dev.binclub.paperbin

import dev.binclub.paperbin.transformers.*
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.*
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File
import java.io.InputStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import java.util.logging.Logger
import kotlin.collections.ArrayList

/**
 * @author cookiedragon234 23/Apr/2020
 */
object PaperBinInfo {
	val transformers: MutableMap<String, PaperFeatureTransformer> = hashMapOf()
	var enabled = true
	var started = false
	
	init {
		println("Registering transformers...")
		
		transformers["net.minecraft.server.v1_12_R1.MinecraftServer"] = MinecraftServerTransformer
		transformers["net.minecraft.server.v1_12_R1.PlayerConnection"] = PlayerConnectionTransformer
		transformers["net.minecraft.server.v1_12_R1.EntityInsentient"] = EntityInsentientTransformer
		transformers["net.minecraft.server.v1_12_R1.PersistentVillage"] = PersistentVillageTransformer
		transformers["net.minecraft.server.v1_12_R1.GameRules"] = GameRulesTransformer
		transformers["net.minecraft.server.v1_12_R1.Block"] = BlockTransformer
		transformers["net.minecraft.server.v1_12_R1.BlockStationary"] = BlockStationaryTransformer
		transformers["net.minecraft.server.v1_12_R1.BlockFlowing"] = BlockFlowingTransformer
		transformers["net.minecraft.server.v1_12_R1.BlockMagma"] = BlockMagmaTransformer
		transformers["net.minecraft.server.v1_12_R1.BlockLeaves"] = BlockLeavesTransformer
	}
	
	fun onStartup() {
		started = true
		
		val manager = Bukkit.getPluginManager() as SimplePluginManager
		val listeners = SimplePluginManager::class.java.getDeclaredMethod("getEventListeners", Event::class.java::class.java).let {
			it.isAccessible = true
			it.invoke(manager, 	PlayerCommandPreprocessEvent::class.java) as HandlerList
		}
		
		val plugin = Proxy.newProxyInstance(
			this::class.java.classLoader,
			arrayOf(Plugin::class.java),
			object: InvocationHandler {
				override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any {
					if (method.name == "isEnabled") {
						return true
					}
					throw UnsupportedOperationException("Unsupported method: ${method.name}")
				}
			}
		) as Plugin
		
		listeners.register(RegisteredListener(
			object: Listener{},
			EventExecutor { listener, event ->
				event as PlayerCommandPreprocessEvent
				
				if (event.player.isOp) {
					if (event.message.endsWith("binstop")) {
						enabled = false
						event.player.sendMessage("Stopped paperbin")
					} else if (event.message.endsWith("binstart")) {
						enabled = true
						event.player.sendMessage("Started paperbin")
					}
				}
				
			},
			EventPriority.NORMAL,
			plugin,
			true
		))
		
		println("Registered event")
	}
	
	// Disable some of the optimisations if the TPS is doing fine
	fun isTpsHigh(): Boolean {
		return false
		//return Bukkit.getTPS()[0] >= 19
	}
	
	var ticks: Int = 0
}
