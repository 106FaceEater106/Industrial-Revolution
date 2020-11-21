package me.steven.indrev.components.multiblock

import me.steven.indrev.registry.IRRegistry
import me.steven.indrev.registry.MachineRegistry
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

object StationControllerStructureDefinition : StructureDefinition() {

    private val STATION_CONTROLLER_BLOCKS = MachineRegistry.STATION_CONTROLLER_REGISTRY.blocks.values

    override val identifier: String = "controller"
    override val isOptional: Boolean = false
    override val holder: StructureHolder =
        StructureHelper(this)
            .add(BlockPos.ORIGIN) { state -> STATION_CONTROLLER_BLOCKS.contains(state.block) }
            .create("controller")
            .build()

    override val appendices: Array<StructureDefinition> = arrayOf(Fertilizer)

    object Fertilizer : StructureDefinition() {
        private val FERTILIZER_BLOCK = IRRegistry.FERTILIZER_BLOCK.defaultState

        override val identifier: String = "fertilizer"
        override val isOptional: Boolean = true
        override val holder: StructureHolder = createStationHolder(FERTILIZER_BLOCK)
    }

    private fun createStationHolder(state: BlockState) =
        StructureHelper(this)
            .add(BlockPos(0, -1, 0), state)
            .create("top")
            .add(BlockPos(1, 0, 0), state)
            .create("left")
            .add(BlockPos(-1, 0, 0), state)
            .create("right")
            .add(BlockPos(0, 1, 0), state)
            .create("bottom")
            .add(BlockPos(0, 0, -1), state)
            .create("back")
            .build()
}