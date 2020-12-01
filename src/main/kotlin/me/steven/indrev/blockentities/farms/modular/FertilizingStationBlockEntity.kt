package me.steven.indrev.blockentities.farms.modular

import io.netty.buffer.Unpooled
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.blocks.machine.HorizontalFacingMachineBlock
import me.steven.indrev.items.upgrade.Upgrade
import me.steven.indrev.registry.IRRegistry
import me.steven.indrev.utils.component1
import me.steven.indrev.utils.component2
import me.steven.indrev.utils.component3
import net.minecraft.block.Fertilizable
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.BoneMealItem
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class FertilizingStationBlockEntity : BaseStationBlockEntity(9, IRRegistry.FERTILIZER_BLOCK_ENTITY_TYPE) {

    private val speed = IndustrialRevolution.CONFIG.machines.fertilizerSpeed
    private var queuedBlocks = emptyList<BlockPos>().iterator()
    private var fertilizedBlocks = hashSetOf<BlockPos>()
    private var cooldown = 0.0

    override fun tick(controllerPos: BlockPos, controller: StationControllerBlockEntity, upgrades: Map<Upgrade, Int>): Boolean {
        val isSpreading = (IndustrialRevolution.CONFIG.machines.fastSpread && controller.ticks % 4 == 0 && queuedBlocks.hasNext())
        if (!isSpreading) {
            cooldown += Upgrade.getSpeed(upgrades, controller)
            if (cooldown < speed) return false
        }
        if (!queuedBlocks.hasNext()) {
            fertilizedBlocks.clear()
            val direction = controller.cachedState[HorizontalFacingMachineBlock.HORIZONTAL_FACING]
            queuedBlocks = listOf(controllerPos.offset(direction)).iterator()
        } else {
            val workingArea = controller.getWorkingArea()
            val boneMeals = invStackList.map { it }.filter { it.item is BoneMealItem }.toTypedArray()
            var currentSlot = 0
            val list = hashSetOf<BlockPos>()
            for (blockPos in queuedBlocks) {
                if (currentSlot >= boneMeals.size) {
                    queuedBlocks = emptyList<BlockPos>().iterator()
                    break
                }
                val blockState = world?.getBlockState(blockPos)
                val block = blockState?.block
                val stack = boneMeals[currentSlot]
                if (block is Fertilizable && block.isFertilizable(world, blockPos, blockState, false)) {
                    block.grow(world as ServerWorld, world?.random, blockPos, blockState)
                    world?.syncWorldEvent(2005, blockPos, 5)
                    stack.decrement(1)
                }
                if (stack.isEmpty)
                    currentSlot++
                fertilizedBlocks.add(blockPos)
                Direction.values().forEach { dir ->
                    val offset = blockPos.offset(dir)
                    val (x, y, z) = offset
                    if (!fertilizedBlocks.contains(offset)
                        && workingArea.contains(x.toDouble(), y.toDouble(), z.toDouble()))
                        list.add(offset)
                }
            }
            queuedBlocks = list.iterator()
        }
        cooldown = 0.0
        return true
    }

    override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?): Boolean = stack.item is BoneMealItem

    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?): Boolean = false

    override fun getContainerName(): Text = TranslatableText("block.indrev.fertilizer")

    override fun createScreenHandler(syncId: Int, playerInventory: PlayerInventory?): ScreenHandler {
        val buf = PacketByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        return IndustrialRevolution.FERTILIZER_HANDLER.create(syncId, playerInventory, buf)
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf?) {
        buf?.writeBlockPos(pos)
    }
}