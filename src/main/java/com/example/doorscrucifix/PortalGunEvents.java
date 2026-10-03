package com.example.doorscrucifix;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CrucifixMod.MODID)
public class PortalGunEvents {

    // Right-clicking a block with the gun should place a portal, not open chests/doors.
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getItemStack().getItem() instanceof PortalGunItem) {
            e.setUseBlock(Event.Result.DENY);
        }
    }

    // Left-clicking a block with the gun should set the destination, not break the block.
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (e.getItemStack().getItem() instanceof PortalGunItem) {
            if (e.getPlayer() instanceof ServerPlayerEntity) {
                PortalGunItem.trySetDestination((ServerPlayerEntity) e.getPlayer(), e.getItemStack());
            }
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        if (e.getPlayer().getHeldItemMainhand().getItem() instanceof PortalGunItem) {
            e.setCanceled(true);
        }
    }
}
