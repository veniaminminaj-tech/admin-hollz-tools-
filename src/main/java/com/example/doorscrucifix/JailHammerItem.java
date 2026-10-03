package com.example.doorscrucifix;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

/**
 * Right-click a player: jail them (new jail dimension, water buckets / pearls / chorus cleared).
 * Sneak + right-click a jailed player: release them. Operators only.
 */
public class JailHammerItem extends Item {
    public JailHammerItem(Properties props) {
        super(props);
    }

    @Override
    public boolean hasEffect(ItemStack stack) { return true; }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
        if (!(target instanceof ServerPlayerEntity) && !(target instanceof PlayerEntity)) return ActionResultType.PASS;
        if (player.world.isRemote) return ActionResultType.SUCCESS;

        if (!player.hasPermissionLevel(2)) {
            player.sendStatusMessage(new StringTextComponent("Only operators can use the Jail Hammer.")
                    .mergeStyle(TextFormatting.RED), true);
            return ActionResultType.FAIL;
        }
        if (target == player) return ActionResultType.FAIL;

        ServerPlayerEntity victim = (ServerPlayerEntity) target;
        String name = victim.getName().getString();

        if (player.isSneaking()) {
            if (!Jail.isJailed(victim)) {
                player.sendStatusMessage(new StringTextComponent(name + " is not jailed.")
                        .mergeStyle(TextFormatting.YELLOW), true);
                return ActionResultType.SUCCESS;
            }
            Jail.release(victim);
            victim.sendStatusMessage(new StringTextComponent("You have been released.")
                    .mergeStyle(TextFormatting.GREEN), false);
            player.sendStatusMessage(new StringTextComponent("Released " + name + ".")
                    .mergeStyle(TextFormatting.GOLD), true);
            return ActionResultType.SUCCESS;
        }

        if (Jail.jail(victim)) {
            victim.sendStatusMessage(new StringTextComponent("You have been jailed.")
                    .mergeStyle(TextFormatting.RED), false);
            player.sendStatusMessage(new StringTextComponent("Jailed " + name + ". Sneak + right-click to release.")
                    .mergeStyle(TextFormatting.GOLD), true);
        } else {
            player.sendStatusMessage(new StringTextComponent("The jail dimension is not loaded.")
                    .mergeStyle(TextFormatting.RED), true);
        }
        return ActionResultType.SUCCESS;
    }
}
