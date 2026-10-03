package com.example.doorscrucifix;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Client -> server: teleport to (or bring) the chosen player. Validated server-side. */
public class TeleportPacket {
    private final UUID target;
    private final boolean bring;

    public TeleportPacket(UUID target, boolean bring) {
        this.target = target;
        this.bring = bring;
    }

    public static void encode(TeleportPacket p, PacketBuffer buf) {
        buf.writeUniqueId(p.target);
        buf.writeBoolean(p.bring);
    }

    public static TeleportPacket decode(PacketBuffer buf) {
        return new TeleportPacket(buf.readUniqueId(), buf.readBoolean());
    }

    public static void handle(TeleportPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.get().getSender();
            if (sender == null || !sender.hasPermissionLevel(2)) return;
            boolean holding = sender.getHeldItemMainhand().getItem() == CrucifixMod.ADMIN_BIBLE.get()
                    || sender.getHeldItemOffhand().getItem() == CrucifixMod.ADMIN_BIBLE.get();
            if (!holding) return;

            ServerPlayerEntity target = sender.server.getPlayerList().getPlayerByUUID(p.target);
            if (target == null) {
                sender.sendStatusMessage(new StringTextComponent("That player is no longer online.")
                        .mergeStyle(TextFormatting.RED), true);
                return;
            }
            ServerPlayerEntity mover = p.bring ? target : sender;
            ServerPlayerEntity dest = p.bring ? sender : target;

            ServerWorld from = mover.getServerWorld();
            from.spawnParticle(ParticleTypes.PORTAL, mover.getPosX(), mover.getPosY() + 1.0, mover.getPosZ(), 40, 0.4, 0.8, 0.4, 0.1);
            mover.teleport(dest.getServerWorld(), dest.getPosX(), dest.getPosY(), dest.getPosZ(), dest.rotationYaw, dest.rotationPitch);
            ServerWorld to = mover.getServerWorld();
            to.spawnParticle(ParticleTypes.PORTAL, mover.getPosX(), mover.getPosY() + 1.0, mover.getPosZ(), 40, 0.4, 0.8, 0.4, 0.1);
            to.playSound(null, mover.getPosition(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        });
        ctx.get().setPacketHandled(true);
    }
}
