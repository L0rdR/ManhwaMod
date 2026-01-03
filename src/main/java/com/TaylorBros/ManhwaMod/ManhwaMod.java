package com.TaylorBros.ManhwaMod;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(ManhwaMod.MODID)
public class ManhwaMod {
    public static final String MODID = "manhwamod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> SYSTEM_KEY = ITEMS.register("system_key",
            () -> new SystemKeyItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal",
            () -> new ManaCrystalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> MANHWA_TAB = CREATIVE_MODE_TABS.register("manhwa_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(MANA_CRYSTAL.get()))
                    .title(Component.translatable("creativetab.manhwa_tab"))
                    .displayItems((params, output) -> {
                        output.accept(SYSTEM_KEY.get());
                        output.accept(MANA_CRYSTAL.get());
                    })
                    .build());

    public ManhwaMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        Messages.register();
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::addCreative);

        // Register the main mod class to the Forge Event Bus for Commands
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ManaOverlay.class);
    }

    // SINGLE COMMAND REGISTRATION POINT
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Register our unified admin command tree
        SystemCommands.register(event.getDispatcher());

        // If you still have these as separate classes, register them here
        // AwakeningCommand.register(event.getDispatcher());
        // ResetCommand.register(event.getDispatcher());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(SYSTEM_KEY);
            event.accept(MANA_CRYSTAL);
        }
    }
}