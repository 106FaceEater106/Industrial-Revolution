package me.steven.indrev.blocks.machine

import me.steven.indrev.blockentities.farms.modular.BaseStationBlockEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World

class BaseStationBlock(
    settings: Settings,
    private val blockEntityProvider: () -> BlockEntity
) : Block(settings), BlockEntityProvider {

    override fun onUse(
        state: BlockState?,
        world: World,
        pos: BlockPos?,
        player: PlayerEntity,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult {
        if (!world.isClient) {
            val blockEntity = world.getBlockEntity(pos) as? BaseStationBlockEntity ?: return ActionResult.PASS
            player.openHandledScreen(blockEntity)
        }
        return ActionResult.success(world.isClient)
    }

    override fun createBlockEntity(world: BlockView?): BlockEntity? = blockEntityProvider()
}