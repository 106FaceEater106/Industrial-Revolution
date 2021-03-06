package me.steven.indrev.gui.controllers.wrench

import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.widget.WButton
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment
import io.netty.buffer.Unpooled
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.blockentities.MachineBlockEntity
import me.steven.indrev.blocks.machine.FacingMachineBlock
import me.steven.indrev.blocks.machine.HorizontalFacingMachineBlock
import me.steven.indrev.gui.PatchouliEntryShortcut
import me.steven.indrev.gui.controllers.IRGuiController
import me.steven.indrev.gui.widgets.machines.WMachineSideDisplay
import me.steven.indrev.gui.widgets.misc.WText
import me.steven.indrev.utils.TransferMode
import me.steven.indrev.utils.add
import me.steven.indrev.utils.addBookEntryShortcut
import me.steven.indrev.utils.identifier
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction

class WrenchController(syncId: Int, playerInventory: PlayerInventory, ctx: ScreenHandlerContext) :
    IRGuiController(
        IndustrialRevolution.WRENCH_HANDLER,
        syncId,
        playerInventory,
        ctx
    ), PatchouliEntryShortcut {

    private var isItemConfig = true
    private val displays = mutableMapOf<Direction, WMachineSideDisplay>()

    init {
        val root = WGridPanel()
        setRootPanel(root)
        root.setSize(96, 120)
        ctx.run { world, pos ->
            val blockEntity = world.getBlockEntity(pos)
            val blockState = world.getBlockState(pos)
            if (blockEntity is MachineBlockEntity<*>) {
                val toggle = WButton(TranslatableText("item.indrev.wrench.item"))
                isItemConfig = blockEntity.inventoryComponent != null
                toggle.setOnClick {
                    isItemConfig = !isItemConfig
                    updateMachineDisplays(
                        if (isItemConfig) blockEntity.inventoryComponent!!.itemConfig
                        else blockEntity.fluidComponent!!.transferConfig
                    )
                    toggle.label = TranslatableText(
                        if (isItemConfig) "item.indrev.wrench.item"
                        else "item.indrev.wrench.fluid"
                    )
                }
                if (blockEntity.inventoryComponent != null && blockEntity.fluidComponent != null)
                    root.add(toggle, 1.7, 0.9)
                toggle.setSize(30, 20)
                val titleWidget = WText(
                    TranslatableText("item.indrev.wrench.title")
                    , HorizontalAlignment.LEFT, 0x404040)
                root.add(titleWidget, 0.0, 0.2)


                val inventoryComponent = blockEntity.inventoryComponent
                val fluidComponent = blockEntity.fluidComponent
                val initConfig = inventoryComponent?.itemConfig ?: fluidComponent?.transferConfig ?: return@run
                MachineSide.values().forEach { side ->
                    val facing =
                        when {
                            blockState.contains(HorizontalFacingMachineBlock.HORIZONTAL_FACING) ->
                                blockState[HorizontalFacingMachineBlock.HORIZONTAL_FACING]
                            blockState.contains(FacingMachineBlock.FACING) ->
                                blockState[FacingMachineBlock.FACING]
                            else ->
                                Direction.UP
                        }
                    val direction = offset(facing, side.direction)
                    val mode = getMode(initConfig, direction)
                    val widget = WMachineSideDisplay(identifier("textures/block/machine_block.png"), side, mode)
                    widget.setOnClick {
                        widget.mode = widget.mode.next()
                        if (isItemConfig)
                            inventoryComponent?.itemConfig?.set(direction, widget.mode)
                        else
                            fluidComponent?.transferConfig?.set(direction, widget.mode)
                        val buf = PacketByteBuf(Unpooled.buffer())
                        buf.writeBoolean(isItemConfig)
                        buf.writeBlockPos(pos)
                        buf.writeInt(direction.id)
                        buf.writeInt(widget.mode.ordinal)
                        ClientSidePacketRegistry.INSTANCE.sendToServer(SAVE_PACKET_ID, buf)
                    }
                    displays[direction] = widget
                    root.add(widget, (side.x - 0.3) * 1.2, (side.y + 1.0) * 1.2)
                }
            }
            addBookEntryShortcut(playerInventory, root, -1.4, -0.47)
        }
        root.validate(this)
    }

    private fun updateMachineDisplays(config: Map<Direction, TransferMode>) {
        displays.forEach { (direction, display) ->
            display.mode = config[direction]!!
        }
    }

    override fun getEntry(): Identifier = identifier("tools/wrench")

    override fun getPage(): Int = 0

    override fun addPainters() {
        super.addPainters()
        rootPanel.backgroundPainter = BackgroundPainter.VANILLA
    }

    private fun offset(facing: Direction, side: Direction): Direction =
        when {
            side.axis == Direction.Axis.Y -> side
            facing == Direction.NORTH -> side
            facing == Direction.SOUTH -> side.rotateYClockwise().rotateYClockwise()
            facing == Direction.WEST -> side.rotateYCounterclockwise()
            facing == Direction.EAST -> side.rotateYClockwise()
            else -> side
        }

    private fun getMode(config: Map<Direction, TransferMode>, side: Direction): TransferMode = config[side]
        ?: TransferMode.NONE

    enum class MachineSide(val x: Int, val y: Int, val direction: Direction, val u1: Float, val v1: Float, val u2: Float, val v2: Float) {
        NORTH(2, 2, Direction.NORTH, 5.333f, 5.333f, 10.666f, 10.666f),
        EAST(1, 2, Direction.EAST, 0.0f, 5.333f, 5.332f, 10.666f),
        SOUTH(3, 3, Direction.SOUTH, 10.667f, 10.667f, 16.0f, 16f),
        WEST(3, 2, Direction.WEST, 10.667f, 5.333f, 16.0f, 10.665f),
        UP(2, 1, Direction.UP, 5.333f, 0.0f, 10.666f, 5.333f),
        DOWN(2, 3, Direction.DOWN, 5.333f, 10.667f, 10.666f, 15.998f)
    }

    companion object {
        val SCREEN_ID = identifier("wrench_item_io_screen")
        val SAVE_PACKET_ID = identifier("save_packet_id")
    }
}