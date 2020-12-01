package me.steven.indrev.blockentities.farms.modular

import me.steven.indrev.api.sideconfigs.ConfigurationType
import me.steven.indrev.api.sideconfigs.SideConfiguration
import me.steven.indrev.items.upgrade.Upgrade
import me.steven.indrev.utils.toIntArray
import me.steven.indrev.utils.transferItems
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.LootableContainerBlockEntity
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Tickable
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

abstract class BaseStationBlockEntity(
    invSize: Int, type: BlockEntityType<*>
) : LootableContainerBlockEntity(type), SidedInventory, Tickable, ExtendedScreenHandlerFactory {

    var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(invSize, ItemStack.EMPTY)

    private val itemConfig = SideConfiguration(ConfigurationType.ITEM)

    private var transferCooldown = 0

    final override fun tick() {
        if (transferCooldown-- <= 0)
            transferItems(this, itemConfig, world!!, pos) { transferCooldown = 12 }
    }

    fun output(toInsert: ItemStack): Boolean {
        val targetStack = (0 until size())
            .map { slot -> getStack(slot) }
            .firstOrNull { invStack -> canCombine(toInsert, invStack) } ?: return addToNewOutputSlot(toInsert).isEmpty
        transfer(toInsert, targetStack)
        if (toInsert.isEmpty) return true
        return addToNewOutputSlot(toInsert).isEmpty
    }

    private fun canCombine(one: ItemStack, two: ItemStack): Boolean
            = one.item === two.item && ItemStack.areTagsEqual(one, two)

    private fun transfer(source: ItemStack, target: ItemStack) {
        val count = this.maxCountPerStack.coerceAtMost(target.maxCount)
        val toTransfer = source.count.coerceAtMost(count - target.count)
        if (toTransfer > 0) {
            target.increment(toTransfer)
            source.decrement(toTransfer)
            markDirty()
        }
    }

    private fun addToNewOutputSlot(stack: ItemStack): ItemStack {
        for (slot in 0 until size()) {
            val itemStack = getStack(slot)
            if (itemStack.isEmpty && canExtract(slot, itemStack, null)) {
                setStack(slot, stack.copy())
                stack.count = 0
                break
            }
        }
        return stack
    }

    abstract fun tick(controllerPos: BlockPos, controller: StationControllerBlockEntity, upgrades: Map<Upgrade, Int>): Boolean

    override fun size(): Int = inventory.size

    override fun getInvStackList(): DefaultedList<ItemStack> = inventory

    override fun setInvStackList(list: DefaultedList<ItemStack>) {
        inventory = list
    }

    override fun getAvailableSlots(side: Direction?): IntArray = (0 until size()).toIntArray()

    override fun toTag(tag: CompoundTag?): CompoundTag {
        itemConfig.toTag(tag)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState?, tag: CompoundTag?) {
        itemConfig.fromTag(tag)
        super.fromTag(state, tag)
    }
}