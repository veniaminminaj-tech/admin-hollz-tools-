package com.example.doorscrucifix;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.server.management.ProfileBanEntry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Date;
import java.util.List;

public class CrucifixItem extends Item {
    private static final int CHARGE_TICKS = 30;   // 1.5s hold
    private static final double RADIUS = 5.0;
    private static final int FREEZE_TICKS = 30;   // 1.5s frozen, then they shatter
    private static final int COOLDOWN = 40;

    public CrucifixItem(Properties props) {
        super(props);
    }

    @Override public UseAction getUseAction(ItemStack stack) { return UseAction.BLOCK; }
    @Override public int getUseDuration(ItemStack stack) { return CHARGE_TICKS; }
    @Override public boolean hasEffect(ItemStack stack) { return true; }

    /** Hold right-click: raise the crucifix and charge. */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.hasPermissionLevel(2)) {
            if (!world.isRemote) {
                player.sendStatusMessage(new StringTextComponent("Only operators can use the Admin Crucifix.")
                        .mergeStyle(TextFormatting.RED), true);
            }
            return ActionResult.resultFail(stack);
        }
        player.setActiveHand(hand);
        return ActionResult.resultConsume(stack);
    }

    /** Charging animation: two spirals of light closing in on the holder. */
    @Override
    public void onUsingTick(ItemStack stack, LivingEntity user, int count) {
        if (!(user.world instanceof ServerWorld)) return;
        ServerWorld sw = (ServerWorld) user.world;
        int t = CHARGE_TICKS - count;
        double r = 2.0 - t * 0.05;
        double angle = t * 0.6;
        for (int i = 0; i < 2; i++) {
            double a = angle + i * Math.PI;
            sw.spawnParticle(ParticleTypes.END_ROD,
                    user.getPosX() + Math.cos(a) * r,
                    user.getPosY() + 0.2 + t * 0.05,
                    user.getPosZ() + Math.sin(a) * r,
                    1, 0, 0, 0, 0.0);
        }
        if (t % 8 == 0) {
            sw.playSound(null, user.getPosition(), SoundEvents.BLOCK_BEACON_AMBIENT,
                    SoundCategory.PLAYERS, 1.0F, 0.6F + t * 0.03F);
        }
    }

    /** Charge complete: expanding light burst that kills every non-player mob in range. */
    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, LivingEntity user) {
        if (world.isRemote || !(user instanceof PlayerEntity) || !(world instanceof ServerWorld)) return stack;
        PlayerEntity player = (PlayerEntity) user;
        ServerWorld sw = (ServerWorld) world;
        if (!player.hasPermissionLevel(2)) return stack;

        sw.playSound(null, user.getPosition(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.2F, 1.0F);
        sw.playSound(null, user.getPosition(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.0F, 1.4F);
        sw.spawnParticle(ParticleTypes.FLASH, user.getPosX(), user.getPosY() + 1.0, user.getPosZ(), 1, 0, 0, 0, 0);

        // expanding rings
        for (int r = 1; r <= (int) RADIUS; r++) {
            int points = r * 3;
            for (int i = 0; i < points; i++) {
                double a = (Math.PI * 2 * i) / points;
                sw.spawnParticle(ParticleTypes.END_ROD,
                        user.getPosX() + Math.cos(a) * r, user.getPosY() + 0.3, user.getPosZ() + Math.sin(a) * r,
                        1, 0, 0.05, 0, 0.02);
            }
        }

        List<MobEntity> mobs = sw.getEntitiesWithinAABB(MobEntity.class,
                user.getBoundingBox().grow(RADIUS),
                e -> e.isAlive() && e.getDistanceSq(user) <= RADIUS * RADIUS);

        int frozen = 0;
        for (MobEntity m : mobs) {
            m.setNoAI(true);
            m.setMotion(Vector3d.ZERO);
            m.getPersistentData().putLong(FreezeEvents.KEY, sw.getGameTime() + FREEZE_TICKS);
            m.getPersistentData().putBoolean(FreezeEvents.KILL_KEY, true);
            sw.spawnParticle(ParticleTypes.ITEM_SNOWBALL, m.getPosX(), m.getPosY() + m.getHeight() / 2, m.getPosZ(),
                    20, 0.3, 0.5, 0.3, 0.05);
            frozen++;
        }
        sw.playSound(null, user.getPosition(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0F, 0.8F);

        player.sendStatusMessage(new StringTextComponent("Froze " + frozen + " mobs - they will shatter.")
                .mergeStyle(TextFormatting.AQUA), true);
        player.getCooldownTracker().setCooldown(this, COOLDOWN);
        return stack;
    }

    /** Sneak + right-click a player: ban them. */
    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
        if (!(target instanceof PlayerEntity) || !player.isSneaking()) return ActionResultType.PASS;
        if (player.world.isRemote) return ActionResultType.SUCCESS;

        if (!player.hasPermissionLevel(2)) {
            player.sendStatusMessage(new StringTextComponent("Only operators can use the Admin Crucifix.")
                    .mergeStyle(TextFormatting.RED), true);
            return ActionResultType.FAIL;
        }
        if (target == player) return ActionResultType.FAIL;

        ServerPlayerEntity victim = (ServerPlayerEntity) target;
        GameProfile profile = victim.getGameProfile();
        String reason = ReasonCommand.getReason(player);
        victim.server.getPlayerList().getBannedPlayers().addEntry(new ProfileBanEntry(
                profile, new Date(), player.getName().getString(), null, reason));

        ServerWorld sw = victim.getServerWorld();
        sw.spawnParticle(ParticleTypes.FLASH, victim.getPosX(), victim.getPosY() + 1.0, victim.getPosZ(), 1, 0, 0, 0, 0);
        sw.spawnParticle(ParticleTypes.SOUL, victim.getPosX(), victim.getPosY() + 1.0, victim.getPosZ(), 40, 0.4, 0.8, 0.4, 0.08);
        sw.playSound(null, victim.getPosition(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.0F, 1.2F);

        victim.connection.disconnect(new StringTextComponent("You have been banned.\nReason: " + reason));
        player.sendStatusMessage(new StringTextComponent("Banned " + profile.getName() + " (" + reason + "). Use /pardon to undo.")
                .mergeStyle(TextFormatting.GOLD), false);
        return ActionResultType.SUCCESS;
    }
}
