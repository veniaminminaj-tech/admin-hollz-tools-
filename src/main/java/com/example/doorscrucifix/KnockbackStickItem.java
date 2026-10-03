package com.example.doorscrucifix;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

/** Hit something to launch it far away in the direction you are looking. Operators only. */
public class KnockbackStickItem extends Item {
    // Minecraft caps velocity packets at ~3.9 blocks/tick, so this is effectively "max".
    private static final double POWER = 3.9;

    public KnockbackStickItem(Properties props) {
        super(props);
    }

    @Override
    public boolean hasEffect(ItemStack stack) { return true; }

    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.world.isRemote || !(attacker instanceof PlayerEntity)
                || !((PlayerEntity) attacker).hasPermissionLevel(2)) {
            return true;
        }
        Vector3d look = attacker.getLookVec();
        double hx = look.x, hz = look.z;
        double len = Math.sqrt(hx * hx + hz * hz);
        if (len < 1.0E-4) { hx = 0; hz = 0; } else { hx /= len; hz /= len; }

        target.setMotion(hx * POWER, 1.0, hz * POWER);
        target.velocityChanged = true;
        if (target instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) target).connection.sendPacket(new SEntityVelocityPacket(target));
        }

        ServerWorld sw = (ServerWorld) attacker.world;
        sw.spawnParticle(ParticleTypes.EXPLOSION, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 1, 0, 0, 0, 0);
        sw.spawnParticle(ParticleTypes.CLOUD, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 20, 0.3, 0.3, 0.3, 0.1);
        sw.playSound(null, target.getPosition(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.2F);
        return true;
    }
}
