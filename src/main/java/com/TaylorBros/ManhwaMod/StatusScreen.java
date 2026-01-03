package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StatusScreen extends Screen {
    // ... Window dimensions ...

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // ... Redirect logic ...
        this.renderBackground(guiGraphics);
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        guiGraphics.fill(x, y, x + WINDOW_WIDTH, y + WINDOW_HEIGHT, 0xAA000000);
        guiGraphics.renderOutline(x, y, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF00AAFF);

        // CASCADE: Use constants for Header
        int level = this.minecraft.player.getPersistentData().getInt(SystemData.LEVEL);
        guiGraphics.drawString(this.font, showSkills ? "§b§lSKILL LIST" : "§b§lSYSTEM STATUS", x + 12, y + 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, getRank(level), x + WINDOW_WIDTH - 45, y + 10, 0xFFFFFF);

        if (showSkills) {
            renderSkillsTab(guiGraphics, x, y, mouseX, mouseY);
        } else {
            renderStatsTab(guiGraphics, x, y, level);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    // Inside StatusScreen.java - Stats Tab Cascade
    private void renderStatsTab(GuiGraphics g, int x, int y, int level) {
        int pts = SystemData.getPoints(this.minecraft.player);
        g.drawString(this.font, "§fPoints: §e" + pts, x + 15, y + 65, 0xFFFFFF);

        // Using SystemData helpers ensures logic isn't "undone" elsewhere
        drawStat(g, "Strength:", SystemData.getStrength(this.minecraft.player), "§c", x + 15, y + 80);
        drawStat(g, "Mana:", SystemData.getMana(this.minecraft.player), "§d", x + 15, y + 160);
    }
}