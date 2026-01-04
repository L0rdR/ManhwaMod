package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

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
        this.renderBackground(guiGraphics);
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

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
        // 1. Render Header and Name
        g.drawString(this.font, "§b§lAWAKENED - " + this.minecraft.player.getName().getString().toUpperCase(), x + 12, y + 10, 0xFFFFFF);

       // NEW: Get Level and Rank from NBT
        int level = this.minecraft.player.getPersistentData().getInt("manhwamod.level");
        String rank = this.minecraft.player.getPersistentData().getString("manhwamod.rank");
        if (rank.isEmpty()) rank = "E";

        // Display Rank and Level with color coding
        g.drawString(this.font, "§fRank: " + getRankColor(rank) + rank, x + 15, y + 25, 0xFFFFFF);
        g.drawString(this.font, "§fLevel: §b" + level + "§7/1000", x + 15, y + 35, 0xFFFFFF);

        // Level Progress Bar (Visual)
        g.fill(x + 15, y + 46, x + 175, y + 48, 0xFF444444); // Bar Background
        int levelWidth = (int)((level / 1000.0) * 160);
        g.fill(x + 15, y + 46, x + 15 + levelWidth, y + 48, 0xFF00AAFF); // Blue Level Bar

        // 3. Render Available Points
        int pts = SystemData.getPoints(this.minecraft.player);
        g.drawString(this.font, "§fAvailable Points: §e" + pts, x + 15, y + 65, 0xFFFFFF);

        // 4. Render Core Stats
        drawStat(g, "Strength:", SystemData.getStrength(this.minecraft.player), "§c", x + 15, y + 80);
        drawStat(g, "Health:", SystemData.getHealthStat(this.minecraft.player), "§a", x + 15, y + 100);
        drawStat(g, "Defense:", SystemData.getDefense(this.minecraft.player), "§7", x + 15, y + 120);
        drawStat(g, "Speed:", SystemData.getSpeed(this.minecraft.player), "§f", x + 15, y + 140);

        // 5. Render Mana and Pool (with the fixed 10x capacity display)
        int manaStat = SystemData.getMana(this.minecraft.player);
        int currentMana = SystemData.getCurrentMana(this.minecraft.player);

        drawStat(g, "Mana:", manaStat, "§d", x + 15, y + 160);
        g.drawString(this.font, "§8Pool: " + currentMana + " / " + (manaStat * 10), x + 25, y + 172, 0xFFFFFF);
    }
    // Helper for Rank Colors
    private String getRankColor(String rank) {
        return switch (rank) {
            case "SSS", "SS" -> "§6§l"; // Gold
            case "S" -> "§e§l";         // Yellow
            case "A" -> "§c";           // Red
            case "B" -> "§d";           // Purple
            default -> "§f";            // White/Gray
        };
    }

    private void renderSkillsTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "§b§lUNLOCKED ARTS", x + 12, y + 10, 0xFFFFFF);
        List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);
        int startY = y + 40;

        if (skills.isEmpty()) {
            g.drawString(this.font, "§7No Arts Learned.", x + 20, startY, 0xFFFFFF);
        } else {
            for (int i = 0; i < Math.min(skills.size(), 12); i++) {
                int id = skills.get(i);
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