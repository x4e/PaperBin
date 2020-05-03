package dev.binclub.paperbin

import dev.binclub.paperbin.transformers.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.plugin.EventExecutor
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.RegisteredListener
import org.bukkit.plugin.SimplePluginManager
import java.lang.Compiler.command
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy


/**
 * @author cookiedragon234 23/Apr/2020
 */
object PaperBinInfo {
	val transformers: MutableMap<String, PaperFeatureTransformer> = hashMapOf()
	var enabled = true
	var started = false
	
	init {
		println("Registering transformers...")
		
		for (transformer in arrayOf(
			BlockFlowingTransformer,
			BlockLeavesTransformer,
			BlockMagmaTransformer,
			BlockStationaryTransformer,
			BlockTransformer,
			CraftEventFactoryTransformer,
			EntityInsentientTransformer,
			EntityTransformer,
			EntityVillagerTransformer,
			GameRulesTransformer,
			MinecraftServerTransformer,
			PersistentVillageTransformer,
			PlayerConnectionTransformer
		)) {
			transformers[transformer.target] = transformer
		}
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
