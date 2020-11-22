package me.steven.indrev.blockentities.farms.modular

import me.steven.indrev.blockentities.crafters.UpgradeProvider
import me.steven.indrev.blockentities.farms.AOEMachineBlockEntity
import me.steven.indrev.blocks.machine.HorizontalFacingMachineBlock
import me.steven.indrev.config.BasicMachineConfig
import me.steven.indrev.inventories.inventory
import me.steven.indrev.items.upgrade.Upgrade
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.utils.Tier
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction

class StationControllerBlockEntity(tier: Tier) : AOEMachineBlockEntity<BasicMachineConfig>(tier, MachineRegistry.STATION_CONTROLLER_REGISTRY), UpgradeProvider {

    init {
        this.inventoryComponent = inventory(this) {}
    }

    override var range: Int = 5

    override fun machineTick() {
        val facing = cachedState[HorizontalFacingMachineBlock.HORIZONTAL_FACING]
        Direction.values().forEach { dir ->
            if (dir != facing) {
                val station = world?.getBlockEntity(pos.offset(dir)) as? BaseStationBlockEntity ?: return@forEach
                station.tick(pos, this, getUpgrades(inventoryComponent!!.inventory))
            }
        }
    }

    override fun getWorkingArea(): Box {
        val direction = cachedState[HorizontalFacingMachineBlock.HORIZONTAL_FACING]
        val r = range - 1.0
        return Box(pos.offset(direction, r.toInt() + 1)).stretch(r, 0.0, r).stretch(-r, 0.0, -r)
    }

    override fun getUpgradeSlots(): IntArray = intArrayOf(0, 1, 2, 3)

    override fun getAvailableUpgrades(): Array<Upgrade> = Upgrade.DEFAULT

    override fun getBaseValue(upgrade: Upgrade): Double = when (upgrade) {
        Upgrade.ENERGY -> config.energyCost
        Upgrade.SPEED -> 1.0
        Upgrade.BUFFER -> getBaseBuffer()
        else -> 0.0
    }
}