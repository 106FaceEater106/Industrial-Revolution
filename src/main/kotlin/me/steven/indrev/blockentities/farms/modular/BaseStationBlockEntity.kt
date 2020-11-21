package me.steven.indrev.blockentities.farms.modular

import me.steven.indrev.items.upgrade.Upgrade
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.LootableContainerBlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos

abstract class BaseStationBlockEntity(invSize: Int, type: BlockEntityType<*>) : LootableContainerBlockEntity(type) {
    var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(invSize, ItemStack.EMPTY)

    abstract fun tick(controllerPos: BlockPos, controller: StationControllerBlockEntity, upgrades: Map<Upgrade, Int>)

    override fun size(): Int = inventory.size

    override fun getInvStackList(): DefaultedList<ItemStack> = inventory

    override fun setInvStackList(list: DefaultedList<ItemStack>) {
        inventory = list
    }
}