package me.steven.indrev.gui.controllers.machines

import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.blockentities.farms.AOEMachineBlockEntity
import me.steven.indrev.gui.controllers.IRGuiController
import me.steven.indrev.gui.widgets.misc.WText
import me.steven.indrev.utils.identifier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.TranslatableText

class FertilizerController(
    syncId: Int,
    playerInventory: PlayerInventory,
    ctx: ScreenHandlerContext
) :
    IRGuiController(
        IndustrialRevolution.FERTILIZER_HANDLER,
        syncId,
        playerInventory,
        ctx
    ) {
    private var value = -1
    init {
        val root = WGridPanel()
        setRootPanel(root)
        val title = WText(TranslatableText("block.indrev.fertilizer"), HorizontalAlignment.CENTER, 0x404040)
        root.add(title, 0, 0)

        val inputSlot = WItemSlot.of(blockInventory, 0, 3, 3)
        root.add(inputSlot, 5, 1)

        root.validate(this)
    }

    override fun close(player: PlayerEntity?) {
        super.close(player)
        AOEMachineBlockEntity.sendValueUpdatePacket(value, ctx)
    }

    companion object {
        val SCREEN_ID = identifier("fertilizer_screen")
    }
}