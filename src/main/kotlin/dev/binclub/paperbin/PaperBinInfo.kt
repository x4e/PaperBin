package dev.binclub.paperbin

import dev.binclub.paperbin.transformers.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

/**
 * @author cookiedragon234 23/Apr/2020
 */
object PaperBinInfo {
	val transformers: MutableMap<String, PaperFeatureTransformer> = hashMapOf()
	
	init {
		println("Registering transformers...")
		
		transformers["net.minecraft.server.v1_12_R1.EntityInsentient"] = EntityInsentientTransformer
		transformers["net.minecraft.server.v1_12_R1.BlockLeaves"] = BlockLeavesTransformer
		transformers["net.minecraft.server.v1_12_R1.MinecraftServer"] = MinecraftServerTransformer
		transformers["net.minecraft.server.v1_12_R1.PlayerConnection"] = PlayerConnectionTransformer
		transformers["net.minecraft.server.v1_12_R1.PersistentVillage"] = PersistentVillageTransformer
		transformers["net.minecraft.server.v1_12_R1.GameRules"] = GameRulesTransformer
	}
	
	var ticks: Int = 0
}
