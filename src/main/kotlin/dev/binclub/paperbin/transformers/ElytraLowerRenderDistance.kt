package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperBinInfo
import dev.binclub.paperbin.PaperBinFeature
import dev.binclub.paperbin.utils.insnBuilder
import dev.binclub.paperbin.utils.internalName
import net.minecraft.server.v1_12_R1.*
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode
import java.lang.Math.max
import java.lang.reflect.Field
import java.util.*
import kotlin.math.min
import kotlin.math.sqrt

/**
 * @author Tigermouthbear 6/June/2020
 * @author charlie353535 11 feb 2021
 */
object ElytraLowerRenderDistance: PaperBinFeature {
    override fun registerTransformers() {
        if(!PaperBinConfig.elytraLowerRenderDistance) return

        println("Max view distance in overworld is "+PaperBinConfig.elytraLowerRenderDistanceOverworld)
        println("Max view distance in nether is "+PaperBinConfig.elytraLowerRenderDistanceNether)
        println("Max view distance in end is "+PaperBinConfig.elytraLowerRenderDistanceEnd)
        println("Max view distance while flying is "+PaperBinConfig.elytraLowerRenderDistanceFlying)
        println("Flying speed threshold is "+PaperBinConfig.elytraLowerRenderDistanceFlyingThresold)

        // register view distance listeners
        register("net.minecraft.server.v1_12_R1.PlayerChunkMap") { cn ->
            for(mn in cn.methods) {
                if(mn.name == "movePlayer" && mn.desc == "(Lnet/minecraft/server/v1_12_R1/EntityPlayer;)V") {
                    var target: MethodInsnNode? = null

                    for(insn in mn.instructions) {
                        if(target == null && insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/PlayerChunkMap" && insn.name == "getViewDistance" && insn.desc == "()I") target = insn
                    }

                    mn.instructions.insert(target, insnBuilder {
                        //+VarInsnNode(ALOAD, 0)
                        +VarInsnNode(ALOAD, 1)
                        +MethodInsnNode(INVOKESTATIC, ElytraLowerRenderDistance::class.internalName, "getAlteredViewDistance", "(Lnet/minecraft/server/v1_12_R1/PlayerChunkMap;Lnet/minecraft/server/v1_12_R1/EntityPlayer;)I", false)
                    })
                    mn.instructions.remove(target)
                }

                if(mn.name == "setViewDistance" && mn.desc == "(Lnet/minecraft/server/v1_12_R1/EntityPlayer;IZ)V") {
                    var target: VarInsnNode? = null

                    for(insn in mn.instructions) {
                        if(target == null && insn is VarInsnNode && insn.opcode == ISTORE && insn.`var` == 2) target = insn
                    }

                    mn.instructions.insert(target, insnBuilder {
                        +VarInsnNode(ALOAD, 1)
                        +VarInsnNode(ILOAD, 2)
                        +MethodInsnNode(INVOKESTATIC, ElytraLowerRenderDistance::class.internalName, "setViewDistance", "(Lnet/minecraft/server/v1_12_R1/EntityPlayer;I)I", false)
                        +VarInsnNode(ISTORE, 2)
                    })

                    return@register
                }
            }

            error("Could not find target!")
        }
    }

    @JvmStatic
    fun getAlteredViewDistance(playerChunkMap: PlayerChunkMap, player: EntityPlayer): Int {
        val viewDistance = viewDistanceForWorld(player, playerChunkMap.viewDistance)
        playerChunkMap.updateViewDistance(player, viewDistance)
        return viewDistance
    }

    @JvmStatic
    fun setViewDistance(player: EntityPlayer, viewDistance: Int): Int = viewDistanceForWorld(player, viewDistance)

    @JvmStatic
    fun viewDistanceForWorld(player: EntityPlayer, defaultDistance: Int): Int =
        if (isPlayerFlying(player)) PaperBinConfig.elytraLowerRenderDistanceFlying else when (player.getWorld().worldProvider.dimensionManager.dimensionID) {
            0 -> PaperBinConfig.elytraLowerRenderDistanceOverworld
            -1 -> PaperBinConfig.elytraLowerRenderDistanceNether
            1 -> PaperBinConfig.elytraLowerRenderDistanceEnd
            else -> 4
        }

    @JvmStatic
    fun isPlayerFlying(player: EntityPlayer): Boolean =
        (if (player.bukkitEntity.inventory.armorContents[2] != null) player.bukkitEntity.inventory.armorContents[2].type==org.bukkit.Material.ELYTRA else false) && player.bukkitEntity.velocity.length()>=PaperBinConfig.elytraLowerRenderDistanceFlyingThresold && !player.onGround
}