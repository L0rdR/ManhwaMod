package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StatusScreen extends Screen {
    private static final int WINDOW_WIDTH = 190;
    private static final int WINDOW_HEIGHT = 210;
    private int multiplier = 1;
    private boolean showSkills = false;

    protected StatusScreen() { super(Component.literal("Status Window")); }

    @Override
    protected void init() {
        if (this.minecraft.player != null && SystemData.isAwakened(this.minecraft.player)) {
            this.minecraft.setScreen(new AwakenedStatusScreen());
            return;
        }

        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        this.addRenderableWidget(Button.builder(Component.literal(showSkills ? "VIEW STATS" : "VIEW SKILLS"), (button) -> {
            showSkills = !showSkills;
            this.clearWidgets();
            this.init();
        }).bounds(x + 10, y + WINDOW_HEIGHT - 25, 80, 18).build());

        if (!showSkills) {
            this.addRenderableWidget(Button.builder(Component.literal("x" + multiplier), (button) -> {
                multiplier = (multiplier == 1) ? 10 : (multiplier == 10) ? 100 : 1;
                button.setMessage(Component.literal("x" + multiplier));
            }).bounds(x + 135, y + 10, 45, 20).build());

            int buttonX = x + 160;
            int startY = y + 78;
            addStatButton(buttonX, startY, "STR");
            addStatButton(buttonX, startY + 20, "HP");
            addStatButton(buttonX, startY + 40, "DEF");
            addStatButton(buttonX, startY + 60, "SPD");
            addStatButton(buttonX, startY + 80, "MANA");
        }
    }

    private void addStatButton(int x, int y, String type) {
        this.addRenderableWidget(Button.builder(Component.literal("+"), (button) -> {
            int points = SystemData.getPoints(this.minecraft.player);
            int amount = Math.min(points, multiplier);
            if (amount > 0) Messages.sendToServer(new PacketUpdateStats(amount, type));
        }).bounds(x, y, 20, 18).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft.player == null) return;
        this.renderBackground(guiGraphics);
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        guiGraphics.fill(x, y, x + WINDOW_WIDTH, y + WINDOW_HEIGHT, 0xAA000000);
        guiGraphics.renderOutline(x, y, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF00AAFF);

        int level = this.minecraft.player.getPersistentData().getInt(SystemData.LEVEL);
        guiGraphics.drawString(this.font, showSkills ? "§b§lSKILL LIST" : "§b§lSYSTEM STATUS", x + 12, y + 10, 0xFFFFFF);

        if (showSkills) {
            renderSkillsTab(guiGraphics, x, y, mouseX, mouseY);
        } else {
            renderStatsTab(guiGraphics, x, y, level);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderStatsTab(GuiGraphics g, int x, int y, int level) {
        int xp = this.minecraft.player.getPersistentData().getInt(SystemData.XP);
        int pts = SystemData.getPoints(this.minecraft.player);
        int xpNeeded = 50 + (level * 10);

        g.drawString(this.font, "Level: §f" + level, x + 15, y + 30, 0xFFFFFF);
        g.fill(x + 15, y + 42, x + 175, y + 52, 0xFF222222);
        float progress = (float) Math.min(xp, xpNeeded) / xpNeeded;
        g.fill(x + 15, y + 42, x + 15 + (int)(progress * 160), y + 52, 0xFF00FF00);

        g.drawString(this.font, "§fPoints: §e" + pts, x + 15, y + 65, 0xFFFFFF);
        drawStat(g, "Strength:", SystemData.getStrength(this.minecraft.player), "§c", x + 15, y + 80);
        drawStat(g, "Mana:", SystemData.getMana(this.minecraft.player), "§d", x + 15, y + 160);
    }

    private void renderSkillsTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        for (int i = 1; i <= 5; i++) {
            int skillId = this.minecraft.player.getPersistentData().getInt(SystemData.SLOT_PREFIX + i);
            String recipe = this.minecraft.player.getPersistentData().getString(SystemData.RECIPE_PREFIX + skillId);
            int slotY = y + 40 + (i * 28);
            if (skillId != 0 && !recipe.isEmpty()) {
                g.drawString(this.font, "Slot " + i + ": §b" + recipe.split(":")[0], x + 15, slotY, 0xFFFFFF);
            } else {
                g.drawString(this.font, "§8Slot " + i + ": [EMPTY]", x + 15, slotY + 4, 0xFFFFFF);
            }
        }
    }

    private void drawStat(GuiGraphics g, String label, int val, String color, int x, int y) {
        g.drawString(this.font, "§f" + label, x, y, 0xFFFFFF);
        g.drawString(this.font, color + val, x + 85, y, 0xFFFFFF);
    }
}