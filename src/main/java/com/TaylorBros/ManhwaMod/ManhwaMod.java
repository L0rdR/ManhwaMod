package com.TaylorBros.ManhwaMod;

import com.TaylorBros.ManhwaMod.skills.ManhwaSkill;
import com.TaylorBros.ManhwaMod.ManaOverlay;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;

@Mod(ManhwaMod.MODID)
public class ManhwaMod {
    public static final String MODID = "manhwamod";
    private static final Logger LOGGER = LogUtils.getLogger();

    // --- STANDARD ITEMS (These work fine, keep them) ---
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> SYSTEM_KEY = ITEMS.register("system_key",
            () -> new SystemKeyItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SKILL_ORB = ITEMS.register("skill_orb", () -> new SkillOrbItem(new Item.Properties()));
    public static final RegistryObject<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal",
            () -> new ManaCrystalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> MANHWA_TAB = CREATIVE_MODE_TABS.register("manhwa_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(MANA_CRYSTAL.get()))
                    .title(Component.translatable("creativetab.manhwa_tab"))
                    .displayItems((params, output) -> {
                        output.accept(SYSTEM_KEY.get());
                        output.accept(MANA_CRYSTAL.get());
                        output.accept(SKILL_ORB.get());
                    })
                    .build());

    public ManhwaMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1. Register Standard Content
        ModParticles.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        Messages.register();

        // 2. Register Client Listeners
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
            event.accept(SYSTEM_KEY);
            event.accept(MANA_CRYSTAL);
            event.accept(SKILL_ORB);
        }
    }

    // ======================================================================
    // THE "MANUAL" SKILL REGISTRY
    // This runs ONLY when Forge fires the RegisterEvent. No DeferredRegister crashes.
    // ======================================================================
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class SkillRegistration {

        @SubscribeEvent
        public static void onRegister(RegisterEvent event) {
            // Check for Epic Fight Skill Registry by STRING NAME (Safe)
            if (event.getRegistryKey().location().toString().equals("epicfight:skill")) {

                System.out.println("MANHWAMOD: Found EpicFight Registry! Injecting Skills...");

                for (SkillTags.Shape shape : SkillTags.Shape.values()) {

                    // 1. Trigger Time
                    float trigger = switch(shape) {
                        case BLINK_STRIKE -> 0.4f;
                        case DASH -> 0.2f;
                        case SLASH_BARRAGE, IMPACT_BURST -> 0.75f;
                        default -> 0.6f;
                    };

                    // 2. Name
                    ResourceLocation regName = new ResourceLocation(MODID, "skill_" + shape.name().toLowerCase());

                    // 3. Create Skill
                    Skill skill = new ManhwaSkill(
                            Skill.createBuilder()
                                    .setCategory(SkillCategories.IDENTITY) // Visible in Menu
                                    .setActivateType(Skill.ActivateType.DURATION)
                                    .setResource(Skill.Resource.WEAPON_INNATE_ENERGY)
                                    .setRegistryName(regName),
                            shape,
                            trigger
                    );

                    // 4. Register Manually (Bypass Type Checks)
                    @SuppressWarnings("unchecked")
                    net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<Skill>> skillKey =
                            (net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<Skill>>) event.getRegistryKey();

                    event.register(skillKey, regName, () -> skill);
                    System.out.println("MANHWAMOD: Registered " + regName);
                }
            }
        }
    }
}