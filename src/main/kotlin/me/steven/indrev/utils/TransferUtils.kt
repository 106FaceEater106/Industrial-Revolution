package me.steven.indrev.utils

import alexiil.mc.lib.attributes.fluid.FluidAttributes
import alexiil.mc.lib.attributes.fluid.FluidExtractable
import alexiil.mc.lib.attributes.fluid.FluidInsertable
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil
import alexiil.mc.lib.attributes.item.ItemAttributes
import alexiil.mc.lib.attributes.item.ItemInvUtil
import alexiil.mc.lib.attributes.item.compat.FixedSidedInventoryVanillaWrapper
import me.steven.indrev.api.sideconfigs.SideConfiguration
import me.steven.indrev.components.InventoryComponent
import me.steven.indrev.components.fluid.FluidComponent
import net.minecraft.block.ChestBlock
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

fun transferItems(inventoryComponent: InventoryComponent, pos: BlockPos, world: World, onSuccess: () -> Unit) {
    inventoryComponent.itemConfig.forEach { (direction, mode) ->
        val neighborPos = pos.offset(direction)
        val inventory = inventoryComponent.inventory
        if (mode.output) {
            val neighborInv = getInventory(world, neighborPos)
            if (neighborInv != null) {
                inventory.outputSlots.forEach { slot ->
                    transferItems(inventory, neighborInv, slot, direction, onSuccess)
                }
                return@forEach
            }
            val insertable = ItemAttributes.INSERTABLE.getFirstOrNull(world, neighborPos)
            if (insertable != null) {
                val extractable = FixedSidedInventoryVanillaWrapper.create(inventory, direction).extractable
                ItemInvUtil.move(extractable, insertable, 64)
            }
        }
        if (mode.input) {
            val neighborInv = getInventory(world, neighborPos)
            if (neighborInv != null) {
                getAvailableSlots(neighborInv, direction.opposite).forEach { slot ->
                    transferItems(neighborInv, inventory, slot, direction.opposite, onSuccess)
                }
                return@forEach
            }
            val extractable = ItemAttributes.EXTRACTABLE.getFirstOrNull(world, neighborPos)
            if (extractable != null) {
                val insertable = FixedSidedInventoryVanillaWrapper.create(inventory, direction).insertable
                ItemInvUtil.move(extractable, insertable, 64)
            }
        }
    }
}

fun transferItems(inventory: SidedInventory, config: SideConfiguration, world: World, pos: BlockPos, onSuccess: () -> Unit) {
    config.forEach { (direction, mode) ->
        val neighborPos = pos.offset(direction)
        if (mode.output) {
            val neighborInv = getInventory(world, neighborPos)
            if (neighborInv != null) {
                getAvailableSlots(inventory, direction).forEach { slot ->
                    transferItems(inventory, neighborInv, slot, direction, onSuccess)
                }
                return@forEach
            }
            val insertable = ItemAttributes.INSERTABLE.getFirstOrNull(world, neighborPos)
            if (insertable != null) {
                val extractable = FixedSidedInventoryVanillaWrapper.create(inventory, direction).extractable
                ItemInvUtil.move(extractable, insertable, 64)
            }
        }
        if (mode.input) {
            val neighborInv = getInventory(world, neighborPos)
            if (neighborInv != null) {
                getAvailableSlots(neighborInv, direction.opposite).forEach { slot ->
                    transferItems(neighborInv, inventory, slot, direction.opposite, onSuccess)
                }
                return@forEach
            }
            val extractable = ItemAttributes.EXTRACTABLE.getFirstOrNull(world, neighborPos)
            if (extractable != null) {
                val insertable = FixedSidedInventoryVanillaWrapper.create(inventory, direction).insertable
                ItemInvUtil.move(extractable, insertable, 64)
            }
        }
    }
}

private fun getFirstSlot(inventory: Inventory, predicate: (Int, ItemStack) -> Boolean): Int? =
    (0 until inventory.size()).firstOrNull { slot -> predicate(slot, inventory.getStack(slot)) }

private fun transferItems(from: Inventory, to: Inventory, slot: Int, direction: Direction, onSuccess: () -> Unit) {
    val toTransfer = from.getStack(slot)
    while (!toTransfer.isEmpty) {
        val firstSlot = getFirstSlot(to) { firstSlot, firstStack ->
            (canMergeItems(firstStack, toTransfer) || firstStack.isEmpty)
                    && (to !is SidedInventory || to.canInsert(firstSlot, toTransfer, direction.opposite))
        } ?: break
        val targetStack = to.getStack(firstSlot)
        if (from is SidedInventory && !from.canExtract(slot, toTransfer, direction))
            break
        val availableSize = (toTransfer.maxCount - targetStack.count).coerceAtMost(toTransfer.count)
        if (!targetStack.isEmpty) {
            toTransfer.count -= availableSize
            targetStack.count += availableSize
        } else {
            from.setStack(slot, ItemStack.EMPTY)
            to.setStack(firstSlot, toTransfer)
            break
        }
        onSuccess()
    }
}

private fun getAvailableSlots(inventory: Inventory, side: Direction): IntArray =
    if (inventory is SidedInventory) inventory.getAvailableSlots(side)
    else (0 until inventory.size()).toIntArray()

private fun canMergeItems(first: ItemStack, second: ItemStack): Boolean =
    first.item == second.item
            && first.damage == second.damage
            && first.count < first.maxCount
            && ItemStack.areTagsEqual(first, second)

private fun getInventory(world: World, pos: BlockPos): Inventory? {
    val blockState = world.getBlockState(pos)
    val block = blockState.block
    return when {
        block is InventoryProvider -> block.getInventory(blockState, world, pos)
        block?.hasBlockEntity() == true -> {
            val blockEntity = world.getBlockEntity(pos) as? Inventory ?: return null
            if (blockEntity is ChestBlockEntity && block is ChestBlock)
                ChestBlock.getInventory(block, blockState, world, pos, true)
            else blockEntity
        }
        else -> null
    }
}

fun transferFluids(blockEntity: BlockEntity, fluidComponent: FluidComponent) {
    fluidComponent.tanks.forEach { _ ->
        fluidComponent.transferConfig.forEach innerForEach@{ (direction, mode) ->
            if (mode == TransferMode.NONE) return@innerForEach
            var extractable: FluidExtractable? = null
            var insertable: FluidInsertable? = null
            if (mode.output) {
                insertable = FluidAttributes.INSERTABLE.getAllFromNeighbour(blockEntity, direction).firstOrNull
                    ?: return@innerForEach
                extractable = fluidComponent.extractable
            }
            if (mode.input) {
                extractable = FluidAttributes.EXTRACTABLE.getAllFromNeighbour(blockEntity, direction).firstOrNull ?: return@innerForEach
                insertable = fluidComponent.insertable

            }
            if (extractable != null && insertable != null)
                FluidVolumeUtil.move(extractable, insertable, NUGGET_AMOUNT)
        }
    }
}
