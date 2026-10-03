package com.example.doorscrucifix;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps jailed players in jail, plus /jail and /unjail commands. */
@Mod.EventBusSubscriber(modid = CrucifixMod.MODID)
public class JailEvents {

    /** No chorus fruit / ender pearl escapes. */
    @SubscribeEvent
    public static void onEnderTeleport(EnderTeleportEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity && Jail.isJailed((PlayerEntity) event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }

    /** Once a second, strip forbidden items from jailed players. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        if (event.player.ticksExisted % 20 != 0) return;
        if (Jail.isJailed(event.player)) Jail.clearForbidden(event.player);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        PlayerEntity p = event.getPlayer();
        if (p != null && Jail.isJailed(p) && !p.hasPermissionLevel(2)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof PlayerEntity) {
            PlayerEntity p = (PlayerEntity) event.getEntity();
            if (Jail.isJailed(p) && !p.hasPermissionLevel(2)) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        if (Jail.isJailed(event.getPlayer()) && Jail.isForbidden(event.getItem().getItem().getItem())) {
            event.setCanceled(true);
        }
    }

    /** Dying does not free you: keep the jail data and send them back to the cell. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundNBT from = event.getOriginal().getPersistentData();
        CompoundNBT to = event.getPlayer().getPersistentData();
        for (String key : Jail.ALL_TAGS) {
            if (from.contains(key)) to.put(key, from.get(key).copy());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity
                && event.getPlayer().getPersistentData().getBoolean(Jail.TAG_JAILED)) {
            Jail.jail((ServerPlayerEntity) event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> d = event.getDispatcher();

        d.register(Commands.literal("jail")
                .requires(s -> s.hasPermissionLevel(2))
                .then(Commands.argument("target", EntityArgument.player()).executes(ctx -> {
                    ServerPlayerEntity t = EntityArgument.getPlayer(ctx, "target");
                    if (Jail.jail(t)) {
                        t.sendStatusMessage(new StringTextComponent("You have been jailed.")
                                .mergeStyle(TextFormatting.RED), false);
                        ctx.getSource().sendFeedback(new StringTextComponent("Jailed " + t.getName().getString())
                                .mergeStyle(TextFormatting.GOLD), true);
                        return Command.SINGLE_SUCCESS;
                    }
                    ctx.getSource().sendErrorMessage(new StringTextComponent("The jail dimension is not loaded."));
                    return 0;
                })));

        d.register(Commands.literal("unjail")
                .requires(s -> s.hasPermissionLevel(2))
                .then(Commands.argument("target", EntityArgument.player()).executes(ctx -> {
                    ServerPlayerEntity t = EntityArgument.getPlayer(ctx, "target");
                    Jail.release(t);
                    t.sendStatusMessage(new StringTextComponent("You have been released.")
                            .mergeStyle(TextFormatting.GREEN), false);
                    ctx.getSource().sendFeedback(new StringTextComponent("Released " + t.getName().getString())
                            .mergeStyle(TextFormatting.GOLD), true);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
