package com.TaylorBros.ManhwaMod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;

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

    @Mod.EventBusSubscriber(modid = ManhwaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (Minecraft.getInstance().screen != null) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            boolean isAwakened = SystemData.isAwakened(player);
            boolean isPlayer = SystemData.isSystemPlayer(player);

            if (STATUS_KEY.consumeClick()) {
                if (isPlayer) Minecraft.getInstance().setScreen(new HunterPhoneScreen());
            }

            if (isAwakened) {
                // REPLACED: Calls helper method 'tryCastSkill' instead of sending packet directly
                if (SKILL_1.consumeClick()) tryCastSkill(player, 0);
                if (SKILL_2.consumeClick()) tryCastSkill(player, 1);
                if (SKILL_3.consumeClick()) tryCastSkill(player, 2);
                if (SKILL_4.consumeClick()) tryCastSkill(player, 3);
                if (SKILL_5.consumeClick()) tryCastSkill(player, 4);

                if (DASH_KEY.consumeClick()) SystemEvents.executeDash(player);
            }
        }

        // --- NEW HELPER METHOD ---
        // Prevents sending packets if the skill is on cooldown
        private static void tryCastSkill(Player player, int slotId) {
            // 1. Get Skill ID (Optimization: Don't send packet if slot is empty)
            int skillId = player.getPersistentData().getInt(SystemData.SLOT_PREFIX + slotId);
            if (skillId <= 0) return;

            // 2. Check Cooldown (Optimization: Don't send packet if on cooldown)
            long lastUse = player.getPersistentData().getLong(SystemData.LAST_USE_PREFIX + slotId);
            int cooldown = player.getPersistentData().getInt(SystemData.COOLDOWN_PREFIX + slotId);
            long timePassed = player.level().getGameTime() - lastUse;

            if (timePassed < cooldown) return;

            // 3. Check Mana (Optimization: Don't send packet if too poor)
            int cost = player.getPersistentData().getInt(SystemData.COST_PREFIX + skillId);
            int currentMana = SystemData.getCurrentMana(player);

            if (currentMana < cost) return;

            // 4. Send Packet if Safe
            Messages.sendToServer(new PacketCastSkill(slotId));
        }
    }
}