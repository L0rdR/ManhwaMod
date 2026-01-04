package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.ArrayList;

public class AwakenedStatusScreen extends Screen {
    private static final int WINDOW_WIDTH = 190;
    private static final int WINDOW_HEIGHT = 210;
    private int multiplier = 1;
    private boolean showSkills = false;

    protected AwakenedStatusScreen() { super(Component.literal("Awakened Status")); }

    @Override
    protected void init() {
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // Toggle button between Stats and Skills
        this.addRenderableWidget(Button.builder(Component.literal(showSkills ? "VIEW STATS" : "VIEW SKILLS"), (button) -> {
            showSkills = !showSkills;
            this.clearWidgets();
            this.init();
        }).bounds(x + 10, y + WINDOW_HEIGHT - 25, 80, 18).build());

        if (!showSkills) {
            // Multiplier button
            this.addRenderableWidget(Button.builder(Component.literal("x" + multiplier), (button) -> {
                multiplier = (multiplier == 1) ? 10 : (multiplier == 10) ? 100 : 1;
                button.setMessage(Component.literal("x" + multiplier));
            }).bounds(x + 135, y + 10, 45, 20).build());

            // Stat Upgrade Buttons
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
        this.renderBackground(guiGraphics);

        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // Main Window Box
        guiGraphics.fill(x, y, x + WINDOW_WIDTH, y + WINDOW_HEIGHT, 0xAA000000);
        guiGraphics.renderOutline(x, y, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF00AAFF);

        if (showSkills) {
            renderSkillsTab(guiGraphics, x, y);
        } else {
            renderStatsTab(guiGraphics, x, y);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderStatsTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "§b§lSYSTEM - " +  this.minecraft.player.getName().getString().toUpperCase(), x + 12, y + 10, 0xFFFFFF);

        // Get live data from the player's NBT
        int pts = SystemData.getPoints(this.minecraft.player);
        int currentMana = SystemData.getCurrentMana(this.minecraft.player);
        int maxMana = SystemData.getMana(this.minecraft.player); // The Stat itself is the Max (e.g., 200)

        g.drawString(this.font, "§fAvailable Points: §e" + pts, x + 15, y + 65, 0xFFFFFF);

        // Render standard stats
        drawStat(g, "Strength:", SystemData.getStrength(this.minecraft.player), "§c", x + 15, y + 80);
        drawStat(g, "Health:", SystemData.getHealthStat(this.minecraft.player), "§a", x + 15, y + 100);
        drawStat(g, "Defense:", SystemData.getDefense(this.minecraft.player), "§7", x + 15, y + 120);
        drawStat(g, "Speed:", SystemData.getSpeed(this.minecraft.player), "§f", x + 15, y + 140);

        // THE MANA DISPLAY: Shows "150 / 200"
        g.drawString(this.font, "§fMana:", x + 15, y + 160, 0xFFFFFF);
    }

    private void renderSkillsTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "§b§lUNLOCKED ARTS", x + 12, y + 10, 0xFFFFFF);

        List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);
        int startY = y + 40;

        if (skills.isEmpty()) {
            g.drawString(this.font, "§7No Arts Learned.", x + 20, startY, 0xFFFFFF);
        } else {
            // Loop through the list and display names
            for (int i = 0; i < skills.size(); i++) {
                if (i > 8) break; // Prevent overflow off screen

                int id = skills.get(i);
                // We access the raw NBT directly here for client-side rendering
                String recipe = this.minecraft.player.getPersistentData().getString("manhwamod.skill_recipe_" + id);
                String name = SkillEngine.getSkillName(recipe);

                g.drawString(this.font, "§e- " + name, x + 15, startY + (i * 12), 0xFFFFFF);
            }
        }
    }

    private void drawStat(GuiGraphics g, String label, int val, String color, int x, int y) {
        g.drawString(this.font, "§f" + label, x, y, 0xFFFFFF);
        g.drawString(this.font, color + val, x + 85, y, 0xFFFFFF);
    }
}