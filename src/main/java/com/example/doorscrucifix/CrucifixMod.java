package com.example.doorscrucifix;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(CrucifixMod.MODID)
public class CrucifixMod {
    public static final String MODID = "doorscrucifix";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    private static Item.Properties props() {
        return new Item.Properties().group(ItemGroup.COMBAT).maxStackSize(1).rarity(Rarity.EPIC);
    }

    public static final RegistryObject<Item> ADMIN_CRUCIFIX = ITEMS.register("admin_crucifix", () -> new CrucifixItem(props()));
    public static final RegistryObject<Item> KNOCKBACK_STICK = ITEMS.register("knockback_stick", () -> new KnockbackStickItem(props()));
    public static final RegistryObject<Item> ADMIN_GUN = ITEMS.register("admin_gun", () -> new AdminGunItem(props()));
    public static final RegistryObject<Item> ADMIN_BIBLE = ITEMS.register("admin_bible", () -> new BibleItem(props()));
    public static final RegistryObject<Item> JAIL_HAMMER = ITEMS.register("jail_hammer", () -> new JailHammerItem(props()));
    public static final RegistryObject<Item> PORTAL_GUN = ITEMS.register("portal_gun",
            () -> new PortalGunItem(new Item.Properties().group(ItemGroup.TOOLS).maxStackSize(1)));

    public CrucifixMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
