package com.TaylorBros.ManhwaMod;

import com.TaylorBros.ManhwaMod.item.MartialScrollItem;
import com.TaylorBros.ManhwaMod.skills.FormlessArtSkill;
import com.TaylorBros.ManhwaMod.ManaOverlay;
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
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import yesman.epicfight.api.data.reloader.SkillManager; // API Import
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;

@Mod(ManhwaMod.MODID)
public class ManhwaMod {
    public static final String MODID = "manhwamod";
    private static final Logger LOGGER = LogUtils.getLogger();

    // --- REGISTRIES ---
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> SYSTEM_KEY = ITEMS.register("system_key", () -> new SystemKeyItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SKILL_ORB = ITEMS.register("skill_orb", () -> new SkillOrbItem(new Item.Properties()));
    public static final RegistryObject<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal", () -> new ManaCrystalItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MARTIAL_SCROLL = ITEMS.register("martial_scroll", () -> new MartialScrollItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> MANHWA_TAB = CREATIVE_MODE_TABS.register("manhwa_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(MANA_CRYSTAL.get()))
                    .title(Component.translatable("creativetab.manhwa_tab"))
                    .displayItems((params, output) -> {
                        output.accept(SYSTEM_KEY.get());
                        output.accept(MANA_CRYSTAL.get());
                        output.accept(SKILL_ORB.get());
                        output.accept(MARTIAL_SCROLL.get());
                    })
                    .build());

    public ManhwaMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1. Register Standard Items (DeferredRegister)
        ModParticles.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        Messages.register();

        // 2. REGISTER SKILLS (IMMEDIATELY IN CONSTRUCTOR)
        // This puts the skill in the registry queue BEFORE it freezes.
        System.out.println("MANHWAMOD: Registering Formless Art...");

        SkillManager.register(
                FormlessArtSkill::new,
                Skill.createBuilder()
                        .setCategory(SkillCategories.IDENTITY)
                        .setActivateType(Skill.ActivateType.DURATION)
                        .setResource(Skill.Resource.NONE),
                MODID,
                "formless_art"
        );

        // 3. Setup Listeners
        modEventBus.addListener(this::addCreative);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ManaOverlay.class);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SystemCommands.register(event.getDispatcher());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(MARTIAL_SCROLL);
        }
    }
}