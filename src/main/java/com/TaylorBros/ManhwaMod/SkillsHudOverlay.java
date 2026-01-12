package com.TaylorBros.ManhwaMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class SkillsHudOverlay {
    public static final IGuiOverlay HUD_SKILLS = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var font = mc.font;
        int x = 10;
        int y = height / 2 - 50;

        for (int i = 0; i < 5; i++) {
            String recipe = SystemData.getSkillRecipe(mc.player, i);
            boolean isEmpty = recipe == null || recipe.isEmpty() || recipe.equals("0");
            int rowY = y + (i * 20); // Spaced out slightly more for the bars

            // 1. Draw Translucent Background (Hex: 0x44000000 -> 44 is Alpha/Transparency)
            guiGraphics.fill(x - 2, rowY - 2, x + 120, rowY + 14, 0x44000000);

            if (!isEmpty) {
                // Get Skill ID from the slot to find its specific cooldown
                int skillId = mc.player.getPersistentData().getInt(SystemData.SLOT_PREFIX + i);

                // Read the synced values
                long unlockTime = mc.player.getPersistentData().getLong("manhwamod.cd_timer_" + skillId);
                int totalDuration = mc.player.getPersistentData().getInt("manhwamod.cd_duration_" + skillId);
                long timeLeft = unlockTime - mc.level.getGameTime();

                // 1. Flash Effect (If cooldown just finished or is active)
                if (timeLeft > 0) {
                    // 2. Cooldown Bar (Yellow bar shrinking)
                    float pct = (float) timeLeft / totalDuration;
                    int barWidth = (int) (pct * 122); // 122 is the max width
                    guiGraphics.fill(x - 2, rowY + 12, x - 2 + barWidth, rowY + 14, 0xFFFFD700);
                }
                else if (timeLeft > -5 && timeLeft <= 0) {
                    // Flash White for 5 ticks when ready
                    guiGraphics.fill(x - 2, rowY - 2, x + 120, rowY + 14, 0x88FFFFFF);
                }

                String displayName = "§b" + (i + 1) + ": " + SkillEngine.getSkillName(recipe);
                guiGraphics.drawString(font, displayName, x, rowY, 0xFFFFFF);
            } else {
                guiGraphics.drawString(font, "§8" + (i + 1) + ": [ EMPTY ]", x, rowY, 0xFFFFFF);
            }
        }
    };
}