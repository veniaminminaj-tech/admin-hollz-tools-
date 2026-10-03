package com.example.doorscrucifix;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Right-click to open a list of every online player and teleport to them. Operators only. */
public class BibleItem extends Item {
    public BibleItem(Properties props) {
        super(props);
    }

    @Override
    public boolean hasEffect(ItemStack stack) { return true; }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.hasPermissionLevel(2)) {
            if (!world.isRemote) {
                player.sendStatusMessage(new StringTextComponent("Only operators can use the Admin Bible.")
                        .mergeStyle(TextFormatting.RED), true);
            }
            return ActionResult.resultFail(stack);
        }
        if (world.isRemote) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openBible);
        }
        return ActionResult.resultSuccess(stack);
    }
}
