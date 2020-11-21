package me.steven.indrev.gui.controllers.machines

import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WSlider
import io.github.cottonmc.cotton.gui.widget.data.Axis
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.blockentities.farms.AOEMachineBlockEntity
import me.steven.indrev.gui.controllers.IRGuiController
import me.steven.indrev.gui.widgets.misc.WText
import me.steven.indrev.utils.add
import me.steven.indrev.utils.configure
import me.steven.indrev.utils.identifier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.TranslatableText

class StationControllerController(
    syncId: Int,
    playerInventory: PlayerInventory,
    ctx: ScreenHandlerContext
) :
    IRGuiController(
        IndustrialRevolution.STATION_CONTROLLER_HANDLER,
        syncId,
        playerInventory,
        ctx
    ) {
    private var value = -1
    init {
        val root = WGridPanel()
        setRootPanel(root)
        configure("block.indrev.station_controller", ctx, playerInventory, blockInventory)

        val slider = WSlider(1, 10, Axis.HORIZONTAL)
        root.add(slider, 1.6, 4.0)
        slider.setSize(50, 20)
        ctx.run { world, pos ->
            val blockEntity = world.getBlockEntity(pos) as? AOEMachineBlockEntity<*> ?: return@run
            slider.value = blockEntity.range
        }
        slider.setValueChangeListener { newValue -> this.value = newValue }

        val text = WText({
            TranslatableText("block.indrev.aoe.range", slider.value)
        }, HorizontalAlignment.LEFT)
        root.add(text, 1.8, 3.7)

        root.validate(this)
    }

    override fun close(player: PlayerEntity?) {
        super.close(player)
        AOEMachineBlockEntity.sendValueUpdatePacket(value, ctx)
    }

    companion object {
        val SCREEN_ID = identifier("station_controller_screen")
    }
}