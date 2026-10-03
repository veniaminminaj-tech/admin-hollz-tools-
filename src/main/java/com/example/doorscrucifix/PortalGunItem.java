package com.example.doorscrucifix;

import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * LEFT CLICK  = save current position + dimension as the destination.
 * RIGHT CLICK = open a portal in front of you, linked to the saved destination.
 */
public class PortalGunItem extends Item {
    private static final String K_HAS = "HasDest";
    private static final String K_DIM = "DestDim";
    private static final String K_X = "DestX";
    private static final String K_Y = "DestY";
    private static final String K_Z = "DestZ";
    private static final String K_YAW = "DestYaw";

    public PortalGunItem(Properties props) {
        super(props);
    }

    // Fires on the server whenever the player swings (left click on air, block or entity).
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof ServerPlayerEntity) {
            trySetDestination((ServerPlayerEntity) entity, stack);
        }
        return false;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && player instanceof ServerPlayerEntity) {
            tryPlacePortal((ServerPlayerEntity) player, stack);
        }
        return ActionResult.resultConsume(stack);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext ctx) {
        PlayerEntity player = ctx.getPlayer();
        if (!ctx.getWorld().isRemote && player instanceof ServerPlayerEntity) {
            tryPlacePortal((ServerPlayerEntity) player, ctx.getItem());
        }
        return ActionResultType.CONSUME;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    public static void trySetDestination(ServerPlayerEntity player, ItemStack stack) {
        if (player.getCooldownTracker().hasCooldown(stack.getItem())) return;
        player.getCooldownTracker().setCooldown(stack.getItem(), 10);

        String dim = player.world.getDimensionKey().getLocation().toString();
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putBoolean(K_HAS, true);
        tag.putString(K_DIM, dim);
        tag.putDouble(K_X, player.getPosX());
        tag.putDouble(K_Y, player.getPosY());
        tag.putDouble(K_Z, player.getPosZ());
        tag.putFloat(K_YAW, player.rotationYaw);

        player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, 0.8F, 1.6F);
        player.sendStatusMessage(new StringTextComponent(String.format(Locale.ROOT,
                "Destination set: %s (%.1f, %.1f, %.1f)", dim, player.getPosX(), player.getPosY(), player.getPosZ()))
                .mergeStyle(TextFormatting.GREEN), true);
    }

    public static void tryPlacePortal(ServerPlayerEntity player, ItemStack stack) {
        if (player.getCooldownTracker().hasCooldown(stack.getItem())) return;

        CompoundNBT tag = stack.getTag();
        if (tag == null || !tag.getBoolean(K_HAS)) {
            player.sendStatusMessage(new StringTextComponent("No destination set - left-click to set one.")
                    .mergeStyle(TextFormatting.RED), true);
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) return;

        RegistryKey<World> destKey = RegistryKey.getOrCreateKey(Registry.WORLD_KEY,
                new ResourceLocation(tag.getString(K_DIM)));
        ServerWorld destWorld = server.getWorld(destKey);
        if (destWorld == null) {
            player.sendStatusMessage(new StringTextComponent("Destination dimension no longer exists.")
                    .mergeStyle(TextFormatting.RED), true);
            return;
        }
        ServerWorld here = player.getServerWorld();

        // ----- entrance portal: in front of the player, standing on their feet level -----
        float yawRad = player.rotationYaw * ((float) Math.PI / 180F);
        double lx = -MathHelper.sin(yawRad);
        double lz = MathHelper.cos(yawRad);
        Vector3d start = player.getPositionVec().add(0.0, 1.0, 0.0);
        Vector3d end = start.add(lx * 3.5, 0.0, lz * 3.5);
        BlockRayTraceResult hit = here.rayTraceBlocks(new RayTraceContext(start, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));
        double dist = 3.0;
        if (hit.getType() == RayTraceResult.Type.BLOCK) {
            dist = Math.min(3.0, start.distanceTo(hit.getHitVec()) - 0.7);
        }
        if (dist < 1.0) {
            player.sendStatusMessage(new StringTextComponent("Not enough room in front of you.")
                    .mergeStyle(TextFormatting.RED), true);
            return;
        }
        double ex = player.getPosX() + lx * dist;
        double ey = player.getPosY();
        double ez = player.getPosZ() + lz * dist;
        float entranceYaw = player.rotationYaw;

        // ----- exit portal: at the saved spot, facing the way the player looked when saving -----
        double ox = tag.getDouble(K_X);
        double oy = tag.getDouble(K_Y);
        double oz = tag.getDouble(K_Z);
        float exitYaw = tag.getFloat(K_YAW) + 180.0F;

        PortalEntity entrance = new PortalEntity(ModEntities.PORTAL.get(), here);
        entrance.setLocationAndAngles(ex, ey, ez, entranceYaw, 0.0F);
        entrance.setTarget(destKey, ox, oy, oz, exitYaw);

        PortalEntity exit = new PortalEntity(ModEntities.PORTAL.get(), destWorld);
        exit.setLocationAndAngles(ox, oy, oz, exitYaw, 0.0F);
        exit.setTarget(here.getDimensionKey(), ex, ey, ez, entranceYaw);

        // make sure the destination chunk is loaded before spawning
        destWorld.getChunk(MathHelper.floor(ox) >> 4, MathHelper.floor(oz) >> 4);
        here.addEntity(entrance);
        destWorld.addEntity(exit);

        here.playSound(null, ex, ey, ez, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 0.6F);
        destWorld.playSound(null, ox, oy, oz, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 0.6F);
        player.getCooldownTracker().setCooldown(stack.getItem(), 30);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("Left click: set destination").mergeStyle(TextFormatting.GRAY));
        tooltip.add(new StringTextComponent("Right click: open portal").mergeStyle(TextFormatting.GRAY));
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.getBoolean(K_HAS)) {
            tooltip.add(new StringTextComponent(String.format(Locale.ROOT, "Destination: %s (%.0f, %.0f, %.0f)",
                    tag.getString(K_DIM), tag.getDouble(K_X), tag.getDouble(K_Y), tag.getDouble(K_Z)))
                    .mergeStyle(TextFormatting.GREEN));
        } else {
            tooltip.add(new StringTextComponent("No destination set").mergeStyle(TextFormatting.DARK_RED));
        }
    }
}
