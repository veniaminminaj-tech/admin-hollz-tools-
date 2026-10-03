package com.example.doorscrucifix;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mobs hit by the Admin Crucifix are frozen in place, then shatter (die) when the freeze time is up.
 * Works after a server restart too, because the timer is saved on the mob.
 */
@Mod.EventBusSubscriber(modid = CrucifixMod.MODID)
public class FreezeEvents {
    public static final String KEY = "doorscrucifix_frozen_until";
    public static final String KILL_KEY = "doorscrucifix_kill_on_thaw";

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity e = event.getEntityLiving();
        if (!(e instanceof MobEntity) || e.world.isRemote || e.ticksExisted % 10 != 0) return;

        CompoundNBT data = e.getPersistentData();
        if (!data.contains(KEY)) return;
        if (!(e.world instanceof ServerWorld)) return;
        ServerWorld sw = (ServerWorld) e.world;

        if (sw.getGameTime() < data.getLong(KEY)) {
            sw.spawnParticle(ParticleTypes.ITEM_SNOWBALL,
                    e.getPosX(), e.getPosY() + e.getHeight() / 2, e.getPosZ(), 3, 0.3, 0.4, 0.3, 0.02);
            return;
        }

        if (data.getBoolean(KILL_KEY)) {
            sw.spawnParticle(ParticleTypes.ITEM_SNOWBALL,
                    e.getPosX(), e.getPosY() + e.getHeight() / 2, e.getPosZ(), 40, 0.4, 0.5, 0.4, 0.1);
            sw.spawnParticle(ParticleTypes.SOUL,
                    e.getPosX(), e.getPosY() + e.getHeight() / 2, e.getPosZ(), 15, 0.3, 0.5, 0.3, 0.05);
            sw.playSound(null, e.getPosition(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.0F, 1.2F);
            e.attackEntityFrom(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
            if (e.isAlive()) e.remove(); // bosses etc.
        } else {
            ((MobEntity) e).setNoAI(false);
            data.remove(KEY);
        }
    }
}
