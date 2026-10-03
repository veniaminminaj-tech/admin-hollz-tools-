package com.example.doorscrucifix;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

/** One end of a portal. Walk into it to be sent to the linked portal. */
public class PortalEntity extends Entity {
    /** How long a portal stays open, in ticks (600 = 30 seconds). */
    public static final int LIFETIME = 600;

    private static final String CD_KEY = "PortalGunCooldown";

    private RegistryKey<World> targetDim;
    private double tx, ty, tz;
    private float targetYaw;

    public PortalEntity(EntityType<? extends PortalEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public void setTarget(RegistryKey<World> dim, double x, double y, double z, float yaw) {
        this.targetDim = dim;
        this.tx = x;
        this.ty = y;
        this.tz = z;
        this.targetYaw = yaw;
    }

    @Override
    public void tick() {
        super.tick();

        if (world.isRemote) {
            spawnClientParticles();
            return;
        }
        if (ticksExisted > LIFETIME || targetDim == null) {
            remove();
            return;
        }
        if (ticksExisted < 8 || ticksExisted > LIFETIME - 8) return; // opening / closing animation

        double rad = Math.toRadians(this.rotationYaw);
        double wx = Math.cos(rad), wz = Math.sin(rad);    // portal width axis
        double nx = -Math.sin(rad), nz = Math.cos(rad);   // portal normal axis
        long now = world.getGameTime();

        List<Entity> list = world.getEntitiesWithinAABB(Entity.class, getBoundingBox().grow(0.3, 0.0, 0.3),
                e -> e != this && e.isAlive() && !e.isPassenger() && !(e instanceof PortalEntity)
                        && (e instanceof LivingEntity || e instanceof ItemEntity));

        for (Entity e : list) {
            if (now < e.getPersistentData().getLong(CD_KEY)) continue;
            if (!(e instanceof ServerPlayerEntity) && !e.canChangeDimension()) continue;

            double dx = e.getPosX() - getPosX(), dz = e.getPosZ() - getPosZ(), dy = e.getPosY() - getPosY();
            double localX = dx * wx + dz * wz;
            double localN = dx * nx + dz * nz;
            if (Math.abs(localX) < 0.9 && Math.abs(localN) < 0.5 && dy > -0.5 && dy < 2.5) {
                send(e, now);
            }
        }
    }

    private void send(Entity e, long now) {
        if (world.getServer() == null) return;
        ServerWorld dest = world.getServer().getWorld(targetDim);
        if (dest == null) return;

        float outYaw = targetYaw + 180.0F;
        double rad = Math.toRadians(outYaw);
        double x = tx - Math.sin(rad) * 1.2;
        double z = tz + Math.cos(rad) * 1.2;
        double y = ty;

        dest.getChunk(MathHelper.floor(x) >> 4, MathHelper.floor(z) >> 4); // force-load

        e.getPersistentData().putLong(CD_KEY, now + 30);
        world.playSound(null, getPosX(), getPosY(), getPosZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.2F);
        e.fallDistance = 0.0F;

        if (e instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) e;
            player.teleport(dest, x, y, z, outYaw, player.rotationPitch);
        } else if (dest == world) {
            e.setLocationAndAngles(x, y, z, outYaw, e.rotationPitch);
            e.setMotion(Vector3d.ZERO);
        } else {
            Entity copy = e.getType().create(dest);
            if (copy != null) {
                copy.copyDataFromOld(e);
                copy.setLocationAndAngles(x, y, z, outYaw, e.rotationPitch);
                copy.setMotion(Vector3d.ZERO);
                e.remove();
                dest.addEntity(copy);
            }
        }
        dest.playSound(null, x, y, z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.2F);
    }

    private void spawnClientParticles() {
        if (ticksExisted < 8 || ticksExisted > LIFETIME - 8) return;
        if (rand.nextInt(2) != 0) return;
        double a = rand.nextDouble() * Math.PI * 2.0;
        double localX = 0.95 * Math.cos(a);
        double localY = 1.5 + 1.45 * Math.sin(a);
        double rad = Math.toRadians(this.rotationYaw);
        world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                getPosX() + Math.cos(rad) * localX, getPosY() + localY, getPosZ() + Math.sin(rad) * localX,
                0.0, 0.02, 0.0);
    }

    @Override
    protected void registerData() {
    }

    @Override
    protected void readAdditional(CompoundNBT tag) {
    }

    @Override
    protected void writeAdditional(CompoundNBT tag) {
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
