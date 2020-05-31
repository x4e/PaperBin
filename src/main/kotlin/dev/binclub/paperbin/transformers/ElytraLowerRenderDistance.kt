package dev.binclub.paperbin.transformers

import dev.binclub.paperbin.PaperBinConfig
import dev.binclub.paperbin.PaperFeature
import dev.binclub.paperbin.utils.insnBuilder
import dev.binclub.paperbin.utils.internalName
import net.minecraft.server.v1_12_R1.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * @author Tigermouthbear 31/May/2020
 */
object ElytraLowerRenderDistance: PaperFeature {
    @JvmStatic
    private val distance = 2

    override fun registerTransformers() {
        if(!PaperBinConfig.elytraLowerRenderDistance) return

        register("net.minecraft.server.v1_12_R1.EntityPlayer") { cn ->
            for(mn in cn.methods) {
                if(mn.name == "getViewDistance" && mn.desc == "()I") {
                    val targets: MutableList<FieldInsnNode> = mutableListOf()

                    for(insn in mn.instructions) {
                        if(insn is FieldInsnNode && insn.opcode == GETFIELD && insn.name == "viewDistance" && insn.desc == "I") targets.add(insn)
                    }

                    for(target in targets) {
                        mn.instructions.insert(target, MethodInsnNode(INVOKESTATIC, ElytraLowerRenderDistance::class.internalName, "getViewDistance", "(Lnet/minecraft/server/v1_12_R1/EntityPlayer;)I", false))
                        mn.instructions.remove(target)
                    }

                    return@register
                }
            }

            error("Could not find target!")
        }

        register("net.minecraft.server.v1_12_R1.PlayerChunkMap") { cn ->
            for(mn in cn.methods) {
                if(mn.name == "movePlayer" && mn.desc == "(Lnet/minecraft/server/v1_12_R1/EntityPlayer;)V") {
                    var target: MethodInsnNode? = null

                    for(insn in mn.instructions) {
                        if(target == null && insn is MethodInsnNode && insn.owner == "net/minecraft/server/v1_12_R1/PlayerChunkMap" && insn.name == "getViewDistance" && insn.desc == "()I") target = insn
                    }

                    mn.instructions.insert(target, insnBuilder {
                        +VarInsnNode(ALOAD, 0)
                        +VarInsnNode(ALOAD, 1)
                        +MethodInsnNode(INVOKESTATIC, ElytraLowerRenderDistance::class.internalName, "setViewDistance", "(Lnet/minecraft/server/v1_12_R1/PlayerChunkMap;Lnet/minecraft/server/v1_12_R1/EntityPlayer;)I", false)
                    })

                    return@register
                }
            }

            error("Could not find target!")
        }
    }

    @JvmStatic
    fun getViewDistance(player: EntityPlayer): Int {
        return if(player.inventory.armor[2].item is ItemElytra) distance else -1
    }

    @JvmStatic
    fun setViewDistance(playerChunkMap: PlayerChunkMap, player: EntityPlayer): Int {
        return if(player.inventory.armor[2].item is ItemElytra) distance else playerChunkMap.viewDistance
    }
}