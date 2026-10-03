package com.example.doorscrucifix;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

/** Everything about the jail dimension: building the cell, jailing, releasing, clearing items. */
public class Jail {
    public static final RegistryKey<World> JAIL_KEY =
            RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(CrucifixMod.MODID, "jail"));

    public static final String TAG_JAILED = "doorscrucifix_jailed";
    public static final String TAG_RET_DIM = "doorscrucifix_return_dim";
    public static final String TAG_RET_X = "doorscrucifix_return_x";
    public static final String TAG_RET_Y = "doorscrucifix_return_y";
    public static final String TAG_RET_Z = "doorscrucifix_return_z";
    public static final String[] ALL_TAGS = {TAG_JAILED, TAG_RET_DIM, TAG_RET_X, TAG_RET_Y, TAG_RET_Z};

    public static boolean isJailed(PlayerEntity player) {
        return player.world.getDimensionKey().equals(JAIL_KEY)
                || player.getPersistentData().getBoolean(TAG_JAILED);
    }

    /** Water buckets (incl. fish buckets, which place water), ender pearls and chorus fruit. */
    public static boolean isForbidden(Item item) {
        return item == Items.WATER_BUCKET || item == Items.COD_BUCKET || item == Items.SALMON_BUCKET
                || item == Items.PUFFERFISH_BUCKET || item == Items.TROPICAL_FISH_BUCKET
                || item == Items.ENDER_PEARL || item == Items.CHORUS_FRUIT;
    }

    public static int clearForbidden(PlayerEntity player) {
        int removed = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && isForbidden(stack.getItem())) {
                removed += stack.getCount();
                player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
            }
        }
        if (removed > 0) player.container.detectAndSendChanges();
        return removed;
    }

    /** Sends the player to the jail cell. Returns false if the jail dimension is not loaded. */
    public static boolean jail(ServerPlayerEntity target) {
        ServerWorld jailWorld = target.server.getWorld(JAIL_KEY);
        if (jailWorld == null) return false;

        buildCell(jailWorld);

        CompoundNBT data = target.getPersistentData();
        if (!data.getBoolean(TAG_JAILED)) {
            data.putString(TAG_RET_DIM, target.world.getDimensionKey().getLocation().toString());
            data.putDouble(TAG_RET_X, target.getPosX());
            data.putDouble(TAG_RET_Y, target.getPosY());
            data.putDouble(TAG_RET_Z, target.getPosZ());
        }
        data.putBoolean(TAG_JAILED, true);

        clearForbidden(target);
        target.teleport(jailWorld, 0.5, 100.0, 0.5, 0.0F, 0.0F);
        return true;
    }

    /** Sends the player back to where they were jailed from. */
    public static void release(ServerPlayerEntity target) {
        CompoundNBT data = target.getPersistentData();

        ServerWorld dest = null;
        if (data.contains(TAG_RET_DIM)) {
            RegistryKey<World> key = RegistryKey.getOrCreateKey(Registry.WORLD_KEY,
                    new ResourceLocation(data.getString(TAG_RET_DIM)));
            dest = target.server.getWorld(key);
        }
        if (dest == null) dest = target.server.getWorld(World.OVERWORLD);

        double x, y, z;
        if (data.contains(TAG_RET_X)) {
            x = data.getDouble(TAG_RET_X);
            y = data.getDouble(TAG_RET_Y);
            z = data.getDouble(TAG_RET_Z);
        } else {
            int sx = dest.getWorldInfo().getSpawnX();
            int sz = dest.getWorldInfo().getSpawnZ();
            x = sx + 0.5;
            z = sz + 0.5;
            y = dest.getHeight(Heightmap.Type.MOTION_BLOCKING, sx, sz);
        }

        for (String tag : ALL_TAGS) data.remove(tag);
        target.teleport(dest, x, y, z, target.rotationYaw, target.rotationPitch);
    }

    /** Builds a 5x4x5 blackstone cell with iron-bar windows and a glowstone light (once). */
    private static void buildCell(ServerWorld world) {
        if (world.getBlockState(new BlockPos(0, 99, 0)).getBlock() == Blocks.POLISHED_BLACKSTONE_BRICKS) return;

        for (int x = -3; x <= 3; x++) {
            for (int y = 99; y <= 104; y++) {
                for (int z = -3; z <= 3; z++) {
                    boolean shell = Math.abs(x) == 3 || Math.abs(z) == 3 || y == 99 || y == 104;
                    world.setBlockState(new BlockPos(x, y, z),
                            shell ? Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
                }
            }
        }
        for (int y = 101; y <= 102; y++) {
            world.setBlockState(new BlockPos(3, y, 0), Blocks.IRON_BARS.getDefaultState(), 3);
            world.setBlockState(new BlockPos(-3, y, 0), Blocks.IRON_BARS.getDefaultState(), 3);
            world.setBlockState(new BlockPos(0, y, 3), Blocks.IRON_BARS.getDefaultState(), 3);
            world.setBlockState(new BlockPos(0, y, -3), Blocks.IRON_BARS.getDefaultState(), 3);
        }
        world.setBlockState(new BlockPos(0, 104, 0), Blocks.GLOWSTONE.getDefaultState(), 3);
        world.setBlockState(new BlockPos(2, 104, 2), Blocks.GLOWSTONE.getDefaultState(), 3);
        world.setBlockState(new BlockPos(-2, 104, -2), Blocks.GLOWSTONE.getDefaultState(), 3);
    }
}
