package com.example.doorscrucifix;

import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Swaps the item model to the glowing one while the crucifix is being held up. */
@Mod.EventBusSubscriber(modid = CrucifixMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.PORTAL.get(), PortalRenderer::new);
        event.enqueueWork(() -> ItemModelsProperties.registerProperty(
                CrucifixMod.ADMIN_CRUCIFIX.get(),
                new ResourceLocation(CrucifixMod.MODID, "charging"),
                (stack, world, entity) -> entity != null && entity.isHandActive()
                        && entity.getActiveItemStack() == stack ? 1.0F : 0.0F));
    }
}
