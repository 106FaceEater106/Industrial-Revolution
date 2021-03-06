package me.steven.indrev

import me.steven.indrev.blockentities.MultiblockBlockEntityRenderer
import me.steven.indrev.blockentities.crafters.CondenserBlockEntityRenderer
import me.steven.indrev.blockentities.crafters.FluidInfuserBlockEntityRenderer
import me.steven.indrev.blockentities.farms.AOEMachineBlockEntityRenderer
import me.steven.indrev.blockentities.modularworkbench.ModularWorkbenchBlockEntityRenderer
import me.steven.indrev.blockentities.storage.ChargePadBlockEntityRenderer
import me.steven.indrev.blockentities.storage.TankBlockEntityRenderer
import me.steven.indrev.blocks.CableModel
import me.steven.indrev.fluids.FluidType
import me.steven.indrev.gui.IRInventoryScreen
import me.steven.indrev.gui.IRModularControllerScreen
import me.steven.indrev.gui.controllers.IRGuiController
import me.steven.indrev.gui.controllers.modular.ModularController
import me.steven.indrev.items.misc.IRTankItemBakedModel
import me.steven.indrev.registry.IRHudRender
import me.steven.indrev.registry.IRRegistry
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.tools.modular.IRModularItem
import me.steven.indrev.utils.Tier
import me.steven.indrev.utils.identifier
import me.steven.indrev.world.chunkveins.VeinType
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.`object`.builder.v1.client.model.FabricModelPredicateProviderRegistry
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry
import net.fabricmc.fabric.api.client.model.ModelVariantProvider
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.client.options.KeyBinding
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.ModelLoader
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.util.Identifier
import net.minecraft.util.collection.WeightedList
import net.minecraft.util.registry.Registry
import org.lwjgl.glfw.GLFW
import java.util.function.Function

@Suppress("UNCHECKED_CAST")
object IndustrialRevolutionClient : ClientModInitializer {
    override fun onInitializeClient() {
        FluidType.WATER.registerReloadListener()
        FluidType.LAVA.registerReloadListener()
        arrayOf(
            IRRegistry.COOLANT_STILL,
            IRRegistry.SULFURIC_ACID_STILL,
            IRRegistry.TOXIC_MUD_STILL
        ).forEach { it.registerRender(FluidType.WATER) }
        arrayOf(
            IRRegistry.MOLTEN_NETHERITE_STILL,
            IRRegistry.MOLTEN_IRON_STILL,
            IRRegistry.MOLTEN_GOLD_STILL,
            IRRegistry.MOLTEN_COPPER_STILL,
            IRRegistry.MOLTEN_TIN_STILL
        ).forEach { it.registerRender(FluidType.LAVA) }
        IRHudRender
        arrayOf(
            IndustrialRevolution.COAL_GENERATOR_HANDLER,
            IndustrialRevolution.SOLAR_GENERATOR_HANDLER,
            IndustrialRevolution.BIOMASS_GENERATOR_HANDLER,
            IndustrialRevolution.HEAT_GENERATOR_HANDLER,
            IndustrialRevolution.BATTERY_HANDLER,
            IndustrialRevolution.ELECTRIC_FURNACE_HANDLER,
            IndustrialRevolution.PULVERIZER_HANDLER,
            IndustrialRevolution.COMPRESSOR_HANDLER,
            IndustrialRevolution.INFUSER_HANDLER,
            IndustrialRevolution.RECYCLER_HANDLER,
            IndustrialRevolution.CHOPPER_HANDLER,
            IndustrialRevolution.RANCHER_HANDLER,
            IndustrialRevolution.MINER_HANDLER,
            IndustrialRevolution.MODULAR_WORKBENCH_HANDLER,
            IndustrialRevolution.FISHING_FARM_HANDLER,
            IndustrialRevolution.WRENCH_HANDLER,
            IndustrialRevolution.SMELTER_HANDLER,
            IndustrialRevolution.CONDENSER_HANDLER,
            IndustrialRevolution.FLUID_INFUSER_HANDLER,
            IndustrialRevolution.FARMER_HANDLER,
            IndustrialRevolution.RESOURCE_REPORT_HANDLER,
            IndustrialRevolution.SAWMILL_HANDLER,
            IndustrialRevolution.ELECTRIC_FURNACE_FACTORY_HANDLER,
            IndustrialRevolution.PULVERIZER_FACTORY_HANDLER,
            IndustrialRevolution.COMPRESSOR_FACTORY_HANDLER,
            IndustrialRevolution.INFUSER_FACTORY_HANDLER
        ).forEach { handler ->
            ScreenRegistry.register(handler) { controller, inv, _ -> IRInventoryScreen(controller, inv.player) }
        }

        MachineRegistry.CHOPPER_REGISTRY.registerBlockEntityRenderer(::AOEMachineBlockEntityRenderer)
        MachineRegistry.RANCHER_REGISTRY.registerBlockEntityRenderer(::AOEMachineBlockEntityRenderer)
        MachineRegistry.FARMER_REGISTRY.registerBlockEntityRenderer(::AOEMachineBlockEntityRenderer)
        MachineRegistry.MODULAR_WORKBENCH_REGISTRY.registerBlockEntityRenderer(::ModularWorkbenchBlockEntityRenderer)
        MachineRegistry.CHARGE_PAD_REGISTRY.registerBlockEntityRenderer(::ChargePadBlockEntityRenderer)
        MachineRegistry.CONDENSER_REGISTRY.registerBlockEntityRenderer(::CondenserBlockEntityRenderer)
        MachineRegistry.FLUID_INFUSER_REGISTRY.registerBlockEntityRenderer(::FluidInfuserBlockEntityRenderer)
        MachineRegistry.INFUSER_FACTORY_REGISTRY.registerBlockEntityRenderer(::MultiblockBlockEntityRenderer)
        MachineRegistry.COMPRESSOR_FACTORY_REGISTRY.registerBlockEntityRenderer(::MultiblockBlockEntityRenderer)
        MachineRegistry.PULVERIZER_FACTORY_REGISTRY.registerBlockEntityRenderer(::MultiblockBlockEntityRenderer)
        MachineRegistry.ELECTRIC_FURNACE_FACTORY_REGISTRY.registerBlockEntityRenderer(::MultiblockBlockEntityRenderer)

        BlockEntityRendererRegistry.INSTANCE.register(IRRegistry.TANK_BLOCK_ENTITY, ::TankBlockEntityRenderer)

        MachineRegistry.MODULAR_WORKBENCH_REGISTRY.setRenderLayer(RenderLayer.getTranslucent())
        BlockRenderLayerMap.INSTANCE.putBlock(IRRegistry.TANK_BLOCK, RenderLayer.getCutout())
        BlockRenderLayerMap.INSTANCE.putBlock(IRRegistry.SULFUR_CRYSTAL_CLUSTER, RenderLayer.getTranslucent())
        MachineRegistry.FISHING_FARM_REGISTRY.setRenderLayer(RenderLayer.getTranslucent())
        MachineRegistry.CABLE_REGISTRY.setRenderLayer(RenderLayer.getTranslucent())

        val identifier = identifier("tank")
        ModelLoadingRegistry.INSTANCE.registerVariantProvider {
            ModelVariantProvider { modelIdentifier, _ ->
                if (modelIdentifier.namespace == identifier.namespace && modelIdentifier.path == identifier.path && modelIdentifier.variant == "inventory") {
                    return@ModelVariantProvider object : UnbakedModel {
                        override fun getModelDependencies(): MutableCollection<Identifier> = mutableListOf()
                        override fun bake(
                            loader: ModelLoader,
                            textureGetter: Function<SpriteIdentifier, Sprite>,
                            rotationScreenHandler: ModelBakeSettings,
                            modelId: Identifier
                        ) = IRTankItemBakedModel

                        override fun getTextureDependencies(
                            unbakedModelGetter: Function<Identifier, UnbakedModel>?,
                            unresolvedTextureReferences: MutableSet<com.mojang.datafixers.util.Pair<String, String>>?
                        ): MutableCollection<SpriteIdentifier> = mutableListOf()
                    }
                }
                return@ModelVariantProvider null
            }
        }

        FabricModelPredicateProviderRegistry.register(
            IRRegistry.GAMER_AXE_ITEM,
            identifier("activate")
        ) predicate@{ stack, _, _ ->
            val tag = stack?.orCreateTag ?: return@predicate 0f
            return@predicate tag.getFloat("Progress")
        }

        ClientSidePacketRegistry.INSTANCE.register(IndustrialRevolution.SYNC_VEINS_PACKET) { ctx, buf ->
            val totalVeins = buf.readInt()
            for (x in 0 until totalVeins) {
                val id = buf.readIdentifier()
                val entriesSize = buf.readInt()
                val outputs = WeightedList<Block>()
                for (y in 0 until entriesSize) {
                    val rawId = buf.readInt()
                    val weight = buf.readInt()
                    val block = Registry.BLOCK.get(rawId)
                    outputs.add(block, weight)
                }
                val minSize = buf.readInt()
                val maxSize = buf.readInt()
                val veinType = VeinType(id, outputs, minSize..maxSize)
                VeinType.REGISTERED[id] = veinType
            }
        }

        ClientSidePacketRegistry.INSTANCE.register(IndustrialRevolution.SYNC_PROPERTY) { ctx, buf ->
            val syncId = buf.readInt()
            val property = buf.readInt()
            val value = buf.readInt()
            ctx.taskQueue.execute {
                val handler = ctx.player.currentScreenHandler
                if (handler.syncId == syncId)
                    (handler as? IRGuiController)?.propertyDelegate?.set(property, value)
            }
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (MODULAR_CONTROLLER_KEYBINDING.wasPressed()) {
                val playerInventory = MinecraftClient.getInstance().player?.inventory ?: break
                val hasModularItem = (0 until playerInventory.size())
                    .associateWith { slot -> playerInventory.getStack(slot) }
                    .filter { (_, stack) -> stack.item is IRModularItem<*> }
                    .isNotEmpty()
                if (hasModularItem)
                    MinecraftClient.getInstance()
                        .openScreen(IRModularControllerScreen(ModularController(client.player!!.inventory)))
            }
        }
        val models = arrayOf(
            CableModel(Tier.MK1), CableModel(Tier.MK2), CableModel(Tier.MK3), CableModel(Tier.MK4)
        )
        ModelLoadingRegistry.INSTANCE.registerVariantProvider {
            ModelVariantProvider { modelIdentifier, _ ->
                if (modelIdentifier.namespace == "indrev" && modelIdentifier.path.contains("cable_mk"))
                    return@ModelVariantProvider models[modelIdentifier.path.last().toString().toInt() - 1]
                return@ModelVariantProvider null
            }
        }
    }

    val MODULAR_CONTROLLER_KEYBINDING = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.indrev.modular",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "category.indrev"
        )
    )
}