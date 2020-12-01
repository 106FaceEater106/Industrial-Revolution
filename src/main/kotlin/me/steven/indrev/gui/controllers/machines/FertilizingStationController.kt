package me.steven.indrev.gui.controllers.machines

import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.gui.controllers.IRGuiController
import me.steven.indrev.gui.widgets.misc.WText
import me.steven.indrev.utils.identifier
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.TranslatableText

class FertilizingStationController(
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

    init {
        val root = WGridPanel()
        setRootPanel(root)
        val title = WText(TranslatableText("block.indrev.fertilizer"), HorizontalAlignment.LEFT, 0x404040)
        root.add(title, 0, 0)

        val inputSlot = WItemSlot.of(blockInventory, 0, 3, 3)
        root.add(inputSlot, 3, 1)

        root.add(createPlayerInventoryPanel(), 0, 5)

        root.validate(this)
    }

    companion object {
        val SCREEN_ID = identifier("fertilizer_screen")
    }
}