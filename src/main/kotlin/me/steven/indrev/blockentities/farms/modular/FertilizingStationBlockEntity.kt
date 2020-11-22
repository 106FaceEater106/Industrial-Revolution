package me.steven.indrev.blockentities.farms.modular

import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.blocks.machine.HorizontalFacingMachineBlock
import me.steven.indrev.items.upgrade.Upgrade
import me.steven.indrev.registry.IRRegistry
import me.steven.indrev.utils.component1
import me.steven.indrev.utils.component2
import me.steven.indrev.utils.component3
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class FertilizingStationBlockEntity : BaseStationBlockEntity(9, IRRegistry.FERTILIZER_BLOCK_ENTITY_TYPE), ExtendedScreenHandlerFactory {

    private val config = IndustrialRevolution.CONFIG.machines.fertilizer
    private var blocksIterator = mutableListOf<BlockPos>().iterator()
    private var fertilizedBlocks = hashSetOf<BlockPos>()
    private var cooldown = 0.0

    override fun tick(controllerPos: BlockPos, controller: StationControllerBlockEntity, upgrades: Map<Upgrade, Int>) {
        cooldown += Upgrade.getSpeed(upgrades, controller)
        if (cooldown < config.processSpeed) return
        if (!blocksIterator.hasNext()) {
            fertilizedBlocks.clear()
            val direction = controller.cachedState[HorizontalFacingMachineBlock.HORIZONTAL_FACING]
            blocksIterator = mutableListOf(controllerPos.offset(direction)).iterator()
        } else {
            val workingArea = controller.getWorkingArea()
            val list = hashSetOf<BlockPos>()
            for (blockPos in blocksIterator) {
                world?.syncWorldEvent(2005, blockPos, 5)
                fertilizedBlocks.add(blockPos)
                Direction.values().forEach { dir ->
                    val offset = blockPos.offset(dir)
                    val (x, y, z) = offset
                    if (!fertilizedBlocks.contains(offset)
                        && workingArea.contains(x.toDouble(), y.toDouble(), z.toDouble()))
                        list.add(offset)
                }
            }
            blocksIterator = list.iterator()
        }
        cooldown = if (blocksIterator.hasNext()) config.processSpeed - 2 else 0.0
    }

    override fun getContainerName(): Text = TranslatableText("block.indrev.fertilizer")

    override fun createScreenHandler(syncId: Int, playerInventory: PlayerInventory?): ScreenHandler
            = IndustrialRevolution.FERTILIZER_HANDLER.create(syncId, playerInventory)

    override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf?) {
        buf?.writeBlockPos(pos)
    }
}