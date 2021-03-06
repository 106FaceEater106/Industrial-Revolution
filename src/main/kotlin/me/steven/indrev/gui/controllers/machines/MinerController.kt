package me.steven.indrev.gui.controllers.machines

import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.gui.PatchouliEntryShortcut
import me.steven.indrev.gui.controllers.IRGuiController
import me.steven.indrev.gui.widgets.misc.WText
import me.steven.indrev.gui.widgets.misc.WTooltipedItemSlot
import me.steven.indrev.utils.add
import me.steven.indrev.utils.configure
import me.steven.indrev.utils.identifier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier

class MinerController(syncId: Int, playerInventory: PlayerInventory, ctx: ScreenHandlerContext) :
    IRGuiController(
        IndustrialRevolution.MINER_HANDLER,
        syncId,
        playerInventory,
        ctx
    ), PatchouliEntryShortcut {
    init {
        val root = WGridPanel()
        setRootPanel(root)
        configure("block.indrev.miner", ctx, playerInventory, blockInventory)

        val outputSlots = WTooltipedItemSlot.of(blockInventory, 1, 3, 3, TranslatableText("gui.indrev.output_slot_type"))
        outputSlots.isInsertingAllowed = false
        root.add(outputSlots, 5.0, 1.0)

        root.add(WText({
            TranslatableText("block.indrev.miner.mined", "${propertyDelegate[3]}%")
        }, HorizontalAlignment.CENTER), 3.0, 3.2)

        val scanSlot = WTooltipedItemSlot.of(blockInventory, 14, TranslatableText("gui.indrev.scan_output_slot_type"))
        root.add(scanSlot, 2.5, 2.0)

        root.validate(this)
    }

    override fun canUse(player: PlayerEntity?): Boolean = true

    override fun getEntry(): Identifier = identifier("machines/miner")

    override fun getPage(): Int = 0

    companion object {
        val SCREEN_ID = identifier("miner")
    }
}