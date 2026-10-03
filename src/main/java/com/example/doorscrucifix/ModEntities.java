package com.example.doorscrucifix;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, CrucifixMod.MODID);

    public static final RegistryObject<EntityType<PortalEntity>> PORTAL = ENTITIES.register("portal",
            () -> EntityType.Builder.<PortalEntity>create(PortalEntity::new, EntityClassification.MISC)
                    .size(2.4F, 3.0F)
                    .disableSerialization()
                    .disableSummoning()
                    .immuneToFire()
                    .setTrackingRange(10)
                    .setUpdateInterval(20)
                    .build(CrucifixMod.MODID + ":portal"));
}
