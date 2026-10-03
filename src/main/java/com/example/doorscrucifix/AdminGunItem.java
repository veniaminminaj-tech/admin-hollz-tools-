package com.example.doorscrucifix;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/** Hitscan gun: right-click to fire an instant beam that deals huge damage. Operators only. */
public class AdminGunItem extends Item {
    private static final double RANGE = 100.0;
    private static final float DAMAGE = 1000.0F;
    private static final int COOLDOWN = 4;

    public AdminGunItem(Properties props) {
        super(props);
    }

    @Override
    public boolean hasEffect(ItemStack stack) { return true; }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.hasPermissionLevel(2)) {
            if (!world.isRemote) {
                player.sendStatusMessage(new StringTextComponent("Only operators can use the Admin Gun.")
                        .mergeStyle(TextFormatting.RED), true);
            }
            return ActionResult.resultFail(stack);
        }
        if (world.isRemote) return ActionResult.resultSuccess(stack);

        Vector3d start = player.getEyePosition(1.0F);
        Vector3d look = player.getLookVec();
        Vector3d end = start.add(look.scale(RANGE));

        BlockRayTraceResult block = world.rayTraceBlocks(new RayTraceContext(
                start, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));
        if (block.getType() != RayTraceResult.Type.MISS) end = block.getHitVec();

        AxisAlignedBB box = player.getBoundingBox().expand(look.scale(RANGE)).grow(1.0);
        EntityRayTraceResult hit = ProjectileHelper.rayTraceEntities(world, player, start, end, box,
                e -> !e.isSpectator() && e.canBeCollidedWith());
        if (hit != null) {
            end = hit.getHitVec();
            Entity target = hit.getEntity();
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), DAMAGE);
        }

        ServerWorld sw = (ServerWorld) world;
        double dist = start.distanceTo(end);
        for (double d = 1.0; d < dist; d += 0.5) {
            Vector3d p = start.add(look.scale(d));
            sw.spawnParticle(ParticleTypes.END_ROD, p.x, p.y - 0.1, p.z, 1, 0, 0, 0, 0);
        }
        sw.spawnParticle(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        sw.playSound(null, player.getPosition(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,
                SoundCategory.PLAYERS, 1.5F, 0.8F);

        player.getCooldownTracker().setCooldown(this, COOLDOWN);
        return ActionResult.resultSuccess(stack);
    }
}
