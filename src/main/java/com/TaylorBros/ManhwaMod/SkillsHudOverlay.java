package com.TaylorBros.ManhwaMod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class SkillsHudOverlay {
    public static final IGuiOverlay HUD_SKILLS = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;


        int x = 10; // Left side of screen
        int y = height / 2 - 50; // Centered vertically

        guiGraphics.drawString(mc.font, "§b§lEQUIPPED ARTS", x, y - 15, 0xFFFFFF);

        for (int i = 0; i < 5; i++) {
            // Using your SLOT_PREFIX "manhwamod.slot_"
            String recipe = player.getPersistentData().getString("manhwamod.skill_recipe_ "+ i);
            String displayName;

            if (recipe == null || recipe.isEmpty() || recipe.equals("0")) {
                displayName = "§7Slot " + (i + 1) + ": §8Empty";
            } else {
                // Use your SkillEngine to format the Tag-based name
                displayName = "§f" + (i + 1) + ": §b" + SkillEngine.getSkillName(recipe);
            }

            // Draw the skill slot
            guiGraphics.fill(x - 2, y + (i * 20) - 2, x + 100, y + (i * 20) + 12, 0x88000000); // Background box
            guiGraphics.drawString(mc.font, displayName, x, y + (i * 20), 0xFFFFFF);
        }
    };
}