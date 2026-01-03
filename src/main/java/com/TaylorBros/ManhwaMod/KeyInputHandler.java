package com.TaylorBros.ManhwaMod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class KeyInputHandler {
    public static final String KEY_CATEGORY_MANHWA = "key.category.manhwamod.titles";

    public static final KeyMapping STATUS_KEY = new KeyMapping("key.manhwamod.open_status", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY_MANHWA);
    public static final KeyMapping DASH_KEY = new KeyMapping("key.manhwamod.dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_CATEGORY_MANHWA);
    public static final KeyMapping QUEST_KEY = new KeyMapping("key.manhwamod.quest", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, KEY_CATEGORY_MANHWA);

    public static final KeyMapping SKILL_1 = new KeyMapping("key.manhwamod.skill_1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_1, KEY_CATEGORY_MANHWA);
    public static final KeyMapping SKILL_2 = new KeyMapping("key.manhwamod.skill_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_2, KEY_CATEGORY_MANHWA);
    public static final KeyMapping SKILL_3 = new KeyMapping("key.manhwamod.skill_3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_3, KEY_CATEGORY_MANHWA);
    public static final KeyMapping SKILL_4 = new KeyMapping("key.manhwamod.skill_4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_4, KEY_CATEGORY_MANHWA);
    public static final KeyMapping SKILL_5 = new KeyMapping("key.manhwamod.skill_5", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_5, KEY_CATEGORY_MANHWA);

    // This internal class handles the MOD bus (Registration)
    @Mod.EventBusSubscriber(modid = ManhwaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(STATUS_KEY);
            event.register(DASH_KEY);
            event.register(QUEST_KEY);
            event.register(SKILL_1);
            event.register(SKILL_2);
            event.register(SKILL_3);
            event.register(SKILL_4);
            event.register(SKILL_5);
        }
    }

    // This internal class handles the FORGE bus (Actual Gameplay Taps)
    @Mod.EventBusSubscriber(modid = ManhwaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (Minecraft.getInstance().screen != null) return;

            // Handle Skills
            if (SKILL_1.consumeClick()) Messages.sendToServer(new PacketCastSkill(1));
            if (SKILL_2.consumeClick()) Messages.sendToServer(new PacketCastSkill(2));
            if (SKILL_3.consumeClick()) Messages.sendToServer(new PacketCastSkill(3));
            if (SKILL_4.consumeClick()) Messages.sendToServer(new PacketCastSkill(4));
            if (SKILL_5.consumeClick()) Messages.sendToServer(new PacketCastSkill(5));

            // Handle Systems
            if (DASH_KEY.consumeClick()) {
                // You might need a packet or direct call for Dash here
                SystemEvents.executeDash(Minecraft.getInstance().player);
            }
            if (STATUS_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new StatusScreen());
            }
        }
    }
}