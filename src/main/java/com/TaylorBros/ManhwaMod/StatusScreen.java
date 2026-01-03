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

    protected StatusScreen() {
        super(Component.literal("Status Window"));
    }

    private String getRank(int level) {
        if (level >= 1000) return "§b§lGOD-RANK";
        if (level >= 800)  return "§6§lS-RANK";
        if (level >= 600)  return "§c§lA-RANK";
        if (level >= 400)  return "§d§lB-RANK";
        if (level >= 200)  return "§a§lC-RANK";
        if (level >= 50)   return "§e§lD-RANK";
        return "§7§lE-RANK";
    }

    @Override
    protected void init() {
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // --- TAB TOGGLE BUTTON ---
        this.addRenderableWidget(Button.builder(Component.literal(showSkills ? "VIEW STATS" : "VIEW SKILLS"), (button) -> {
            showSkills = !showSkills;
            this.clearWidgets();
            this.init();
        }).bounds(x + 10, y + WINDOW_HEIGHT - 25, 80, 18).build());

        if (!showSkills) {
            // MULTIPLIER BUTTON
            this.addRenderableWidget(Button.builder(Component.literal("x" + multiplier), (button) -> {
                multiplier = (multiplier == 1) ? 10 : (multiplier == 10) ? 100 : 1;
                button.setMessage(Component.literal("x" + multiplier));
            }).bounds(x + 135, y + 10, 45, 20).build());

            // STAT BUTTONS
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
            int amountToAdd = Math.min(points, multiplier);
            if (amountToAdd > 0) {
                Messages.sendToServer(new PacketUpdateStats(amountToAdd, type));
            }
        }).bounds(x, y, 20, 18).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // 1. SHARED BACKGROUND & BORDER
        guiGraphics.fill(x, y, x + WINDOW_WIDTH, y + WINDOW_HEIGHT, 0xAA000000);
        guiGraphics.renderOutline(x, y, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF00AAFF);

        // 2. HEADER
        guiGraphics.drawString(this.font, showSkills ? "§b§lSKILL LIST" : "§b§lSYSTEM STATUS", x + 12, y + 10, 0xFFFFFF);
        int level = this.minecraft.player.getPersistentData().getInt("manhwamod.level");
        guiGraphics.drawCenteredString(this.font, getRank(level), x + WINDOW_WIDTH - 45, y + 10, 0xFFFFFF);
        guiGraphics.fill(x + 10, y + 22, x + WINDOW_WIDTH - 10, y + 23, 0x44FFFFFF);

        // 3. TAB CONTENT
        if (showSkills) {
            renderSkillsTab(guiGraphics, x, y, mouseX, mouseY);
        } else {
            renderStatsTab(guiGraphics, x, y, level);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderStatsTab(GuiGraphics g, int x, int y, int level) {
        int xp = this.minecraft.player.getPersistentData().getInt("manhwamod.xp");
        int pts = SystemData.getPoints(this.minecraft.player);
        int xpNeeded = 50 + (level * 10);

        // XP BAR
        g.drawString(this.font, "Level: §f" + level, x + 15, y + 30, 0xFFFFFF);
        g.fill(x + 15, y + 42, x + 175, y + 52, 0xFF222222);
        float progress = Math.min(xp, xpNeeded) / (float)xpNeeded;
        g.fill(x + 15, y + 42, x + 15 + (int)(progress * 160), y + 52, 0xFF00FF00);
        g.drawCenteredString(this.font, xp + " / " + xpNeeded + " XP", x + WINDOW_WIDTH / 2, y + 43, 0xFFFFFF);

        // STATS
        g.drawString(this.font, "§fAvailable Points: §e" + pts, x + 15, y + 65, 0xFFFFFF);
        int statY = y + 80;
        drawStat(g, "Strength:", SystemData.getStrength(this.minecraft.player), "§c", x + 15, statY);
        drawStat(g, "Health:", SystemData.getHealthStat(this.minecraft.player), "§a", x + 15, statY + 20);
        drawStat(g, "Defense:", SystemData.getDefense(this.minecraft.player), "§9", x + 15, statY + 40);
        drawStat(g, "Speed:", SystemData.getSpeed(this.minecraft.player), "§e", x + 15, statY + 60);
        drawStat(g, "Mana:", SystemData.getMana(this.minecraft.player), "§d", x + 15, statY + 80);
    }

    private void renderSkillsTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        for (int i = 1; i <= 5; i++) {
            String recipe = this.minecraft.player.getPersistentData().getString("manhwamod.skill_recipe_" + i);
            int slotY = y + 40 + (i * 28);

            // 1. Draw Slot Background
            g.fill(x + 10, slotY - 5, x + WINDOW_WIDTH - 10, slotY + 20, 0x33FFFFFF);

            if (!recipe.isEmpty()) {
                String[] parts = recipe.split(":"); // SHAPE:ELEMENT:MODIFIER
                int cost = this.minecraft.player.getPersistentData().getInt("manhwamod.skill_cost_" + i);

                // 2. Determine Rarity Color based on Mana Cost
                String color;
                String rarityName;
                if (cost >= 100) { color = "§6§l"; rarityName = "LEGENDARY"; }
                else if (cost >= 70) { color = "§d§l"; rarityName = "EPIC"; }
                else if (cost >= 40) { color = "§b"; rarityName = "RARE"; }
                else if (cost >= 20) { color = "§a"; rarityName = "UNCOMMON"; }
                else { color = "§f"; rarityName = "COMMON"; }

                // 3. Draw Skill Name and Effect
                g.drawString(this.font, "Slot " + i + ": " + color + parts[1] + " " + parts[0], x + 15, slotY, 0xFFFFFF);
                g.drawString(this.font, "§8Effect: §7" + parts[2], x + 15, slotY + 10, 0xFFFFFF);

                // 4. Hover Tooltip (Detailed Stats)
                if (mouseX > x + 10 && mouseX < x + 180 && mouseY > slotY - 5 && mouseY < slotY + 20) {
                    int range = 10 + (cost / 10);
                    String tooltip = color + "§l" + rarityName + "\n" +
                            "§bCost: §f" + cost + " MP\n" +
                            "§bRange: §f" + range + " Blocks\n" +
                            "§bModifier: §e" + parts[2];
                    g.renderTooltip(this.font, Component.literal(tooltip), mouseX, mouseY);
                }
            } else {
                // Locked Slot
                g.drawString(this.font, "§8Slot " + i + ": [LOCKED]", x + 15, slotY + 4, 0xFFFFFF);
            }
        }
    }

    private void drawStat(GuiGraphics g, String label, int val, String color, int x, int y) {
        g.drawString(this.font, "§f" + label, x, y, 0xFFFFFF);
        g.drawString(this.font, color + val, x + 85, y, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}