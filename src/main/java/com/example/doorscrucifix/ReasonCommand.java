package com.example.doorscrucifix;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /reason <text>  - sets the ban reason the Admin Crucifix uses for YOUR bans.
 * /reason         - shows your current reason.
 * Saved on the player, so it survives death and server restarts.
 */
@Mod.EventBusSubscriber(modid = CrucifixMod.MODID)
public class ReasonCommand {
    public static final String DEFAULT_REASON = "Banned with the Admin Crucifix";
    private static final String KEY = "doorscrucifix_ban_reason";
    private static final int MAX_LENGTH = 200;

    public static String getReason(PlayerEntity player) {
        String r = player.getPersistentData().getString(KEY);
        return r.isEmpty() ? DEFAULT_REASON : r;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    /** Keep the saved reason after the player dies / returns from the End. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        String r = event.getOriginal().getPersistentData().getString(KEY);
        if (!r.isEmpty()) {
            event.getPlayer().getPersistentData().putString(KEY, r);
        }
    }

    private static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("reason")
                .requires(src -> src.hasPermissionLevel(2))
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().asPlayer();
                    ctx.getSource().sendFeedback(new StringTextComponent(
                            "Crucifix ban reason: " + getReason(p)).mergeStyle(TextFormatting.GOLD), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().asPlayer();
                            String text = StringArgumentType.getString(ctx, "text").trim();
                            if (text.length() > MAX_LENGTH) text = text.substring(0, MAX_LENGTH);
                            if (text.isEmpty()) text = DEFAULT_REASON;
                            p.getPersistentData().putString(KEY, text);
                            ctx.getSource().sendFeedback(new StringTextComponent(
                                    "Crucifix ban reason set to: " + text).mergeStyle(TextFormatting.GOLD), false);
                            return Command.SINGLE_SUCCESS;
                        })));
    }
}
