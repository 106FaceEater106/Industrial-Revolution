package me.steven.indrev.blockentities.farms.modular

import io.netty.buffer.Unpooled
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.items.upgrade.Upgrade
import me.steven.indrev.registry.IRRegistry
import me.steven.indrev.utils.damage
import me.steven.indrev.utils.map
import me.steven.indrev.utils.toVec3d
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags
import net.minecraft.block.BlockState
import net.minecraft.block.LeavesBlock
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.AxeItem
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.tag.BlockTags
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class ChoppingStationBlockEntity : BaseStationBlockEntity(8, IRRegistry.CHOPPER_BLOCK_ENTITY_TYPE) {

    private val speed = IndustrialRevolution.CONFIG.machines.chopperSpeed
    private var queuedBlocks = emptyList<BlockPos>().iterator()
    private var cooldown = 0.0

    override fun tick(controllerPos: BlockPos, controller: StationControllerBlockEntity, upgrades: Map<Upgrade, Int>): Boolean {
        cooldown += Upgrade.getSpeed(upgrades, controller)
        if (cooldown < speed) return false
        if (!queuedBlocks.hasNext()) {
            queuedBlocks = controller.getWorkingArea().map(::BlockPos).iterator()
        }
        val axeStack = (0 until 2).map { slot -> inventory[slot] }.firstOrNull { stack -> stack.item is AxeItem }  ?: return false
        while (queuedBlocks.hasNext()) {
            val blockPos = queuedBlocks.next()
            val blockState = world!!.getBlockState(blockPos)
            if (tryChop(axeStack, blockPos, blockState)) {
                return true
            }
        }
        return false
    }

    override fun getContainerName(): Text = TranslatableText("block.indrev.chopping_station")

    override fun createScreenHandler(syncId: Int, playerInventory: PlayerInventory?): ScreenHandler {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        return IndustrialRevolution.CHOPPING_STATION_CONTROLLER.create(syncId, playerInventory, buf)
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf?) {
        buf?.writeBlockPos(pos)
    }

    override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?): Boolean =
        (slot == 0 || slot == 1) && stack.item.isIn(FabricToolTags.AXES)

    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?): Boolean = slot >= 2

    private fun tryChop(
        axeStack: ItemStack,
        blockPos: BlockPos,
        blockState: BlockState
    ): Boolean {
        val block = blockState.block
        when {
            block.isIn(BlockTags.LOGS) -> {
                if (!axeStack.damage(1, world!!.random) { stack -> stack.decrement(1) })
                    return false
                world?.breakBlock(blockPos, false)
            }
            block is LeavesBlock -> {
                world?.breakBlock(blockPos, false)
            }
            else -> return false
        }
        val lootBuilder = LootContext.Builder(world as ServerWorld)
            .random(world?.random)
            .parameter(LootContextParameters.ORIGIN, blockPos.toVec3d())
            .parameter(LootContextParameters.TOOL, ItemStack.EMPTY)
        val droppedStacks = blockState.getDroppedStacks(lootBuilder)
        droppedStacks.forEach {
            if (!output(it))
                ItemScatterer.spawn(world, blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(), it)
        }
        return true
    }
}