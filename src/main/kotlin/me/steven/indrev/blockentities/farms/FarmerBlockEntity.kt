package me.steven.indrev.blockentities.farms

import me.steven.indrev.blockentities.crafters.UpgradeProvider
import me.steven.indrev.components.InventoryComponent
import me.steven.indrev.config.BasicMachineConfig
import me.steven.indrev.inventories.IRInventory
import me.steven.indrev.items.upgrade.IRUpgradeItem
import me.steven.indrev.items.upgrade.Upgrade
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.utils.Tier
import me.steven.indrev.utils.forEach
import me.steven.indrev.utils.toIntArray
import me.steven.indrev.utils.toVec3d
import net.minecraft.block.*
import net.minecraft.item.BlockItem
import net.minecraft.item.BoneMealItem
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.loot.context.LootContextTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import team.reborn.energy.Energy

class FarmerBlockEntity(tier: Tier) : AOEMachineBlockEntity<BasicMachineConfig>(tier, MachineRegistry.FARMER_REGISTRY), UpgradeProvider {

    init {
        this.inventoryComponent = InventoryComponent({ this }) {
            IRInventory(18, (1..4).toIntArray(), (5 until 14).toIntArray()) { slot, stack ->
                val item = stack?.item
                when {
                    item is IRUpgradeItem -> getUpgradeSlots().contains(slot)
                    Energy.valid(stack) && Energy.of(stack).maxOutput > 0 -> slot == 0
                    slot in 1 until 14 -> true
                    else -> false
                }
            }
        }
    }

    override var range: Int = 5

    var cooldown = 0.0
    var nextBlocks = mutableListOf<BlockPos>().iterator()

    override fun machineTick() {
        val inventory = inventoryComponent?.inventory ?: return
        val upgrades = getUpgrades(inventory)
        cooldown += Upgrade.getSpeed(upgrades, this)
        if (cooldown < config.processSpeed) return
        val world = world as ServerWorld
        if (nextBlocks.hasNext()) {
            var pos = nextBlocks.next()
            var state = world.getBlockState(pos)
            while (Energy.of(this).simulate().use(config.energyCost) && !tryHarvest(state, pos, world) && nextBlocks.hasNext()) {
                pos = nextBlocks.next()
                state = world.getBlockState(pos)
            }
        } else {
            val set = mutableSetOf<BlockPos>()
            getWorkingArea().forEach { x, y, z ->
                set.add(BlockPos(x, y, z))
            }
            nextBlocks = set.iterator()
        }
        cooldown = 0.0
    }

    fun tryHarvest(state: BlockState, pos: BlockPos, world: ServerWorld): Boolean {
        val block = state.block

        val inventory = inventoryComponent?.inventory
        inventory?.inputSlots?.forEach { slot ->
            val stack = inventory.getStack(slot)
            val item = stack.item
            val isValidSeed = item is BlockItem && (item.block is CropBlock || item.block is StemBlock)
            val isCropBlock = block is CropBlock || block is StemBlock
            val shouldHarvest =
                (block is CropBlock && block.isMature(state) && (item is BlockItem && item.block == block || slot == 4)) || block is GourdBlock
            when {
                item is BoneMealItem && isCropBlock && (block as Fertilizable).isFertilizable(world, pos, state, false) -> {
                    stack.decrement(1)
                    (block as Fertilizable).grow(world, world.random, pos, state)
                    world.syncWorldEvent(2005, pos, 0)
                }
                shouldHarvest -> {
                    if (block is CropBlock && stack.count > 1) {
                        world.setBlockState(pos, block.defaultState)
                        stack.decrement(1)
                    } else {
                        world.setBlockState(pos, Blocks.AIR.defaultState)
                    }
                    val lootTable = world.server.lootManager.getTable(block.lootTableId)
                    val lootContext = LootContext.Builder(world)
                        .random(world.random)
                        .parameter(LootContextParameters.ORIGIN, pos.toVec3d())
                        .parameter(LootContextParameters.BLOCK_STATE, state)
                        .parameter(LootContextParameters.TOOL, ItemStack.EMPTY)
                        .build(LootContextTypes.BLOCK)
                    lootTable.generateLoot(lootContext).forEach { inventory.addStack(it) }
                }
                block == Blocks.AIR && isValidSeed -> {
                    val cropState = (item as BlockItem).block.defaultState
                    if (cropState.canPlaceAt(world, pos)) {
                        world.setBlockState(pos, cropState)
                    }
                }
                else -> return false
            }
        }
        Energy.of(this).use(config.energyCost)
        return true
    }

    override fun getWorkingArea(): Box = Box(pos).expand(range.toDouble(), 0.0, range.toDouble())

    override fun getUpgradeSlots(): IntArray = intArrayOf(14, 15, 16, 17)

    override fun getAvailableUpgrades(): Array<Upgrade> = Upgrade.DEFAULT

    override fun getBaseValue(upgrade: Upgrade): Double = when (upgrade) {
        Upgrade.ENERGY -> config.energyCost
        Upgrade.SPEED -> 1.0
        Upgrade.BUFFER -> getBaseBuffer()
        else -> 0.0
    }
}