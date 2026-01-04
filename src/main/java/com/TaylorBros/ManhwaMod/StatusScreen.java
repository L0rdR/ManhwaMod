package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.ArrayList;

public class StatusScreen extends Screen {
    private static final int WINDOW_WIDTH = 190;
    private static final int WINDOW_HEIGHT = 210;
    private int multiplier = 1;
    private String currentTab = "STATS"; // Possible: STATS, SKILLS, QUESTS
    private int skillScrollOffset = 0 ;

    protected StatusScreen() { super(Component.literal("System Status")); }

    @Override
    protected void init() {
        // TIERED ACCESS: Ensure only System Players are here
        if (this.minecraft.player != null && !SystemData.isSystemPlayer(this.minecraft.player)) {
            this.minecraft.setScreen(new AwakenedStatusScreen());
            return;
        }

        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // --- NAVIGATION BUTTONS ---
        this.addRenderableWidget(Button.builder(Component.literal("STATS"), (button) -> { currentTab = "STATS"; this.rebuild(); })
                .bounds(x + 10, y + WINDOW_HEIGHT - 25, 55, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("SKILLS"), (button) -> { currentTab = "SKILLS"; this.rebuild(); })
                .bounds(x + 67, y + WINDOW_HEIGHT - 25, 55, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("QUESTS"), (button) -> { currentTab = "QUESTS"; this.rebuild(); })
                .bounds(x + 124, y + WINDOW_HEIGHT - 25, 55, 18).build());

        // --- STAT UPGRADE LOGIC ---
        if (currentTab.equals("STATS")) {
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

    private void rebuild() { this.clearWidgets(); this.init(); }

    private void addStatButton(int x, int y, String type) {
        this.addRenderableWidget(Button.builder(Component.literal("+"), (button) -> {
            int points = SystemData.getPoints(this.minecraft.player);
            int amount = Math.min(points, multiplier);
            if (amount > 0) Messages.sendToServer(new PacketUpdateStats(amount, type));
        }).bounds(x, y, 20, 18).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics); // Fixes pink/black texture issue

        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // Draw Main Window
        guiGraphics.fill(x, y, x + WINDOW_WIDTH, y + WINDOW_HEIGHT, 0xAA000000);
        guiGraphics.renderOutline(x, y, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF00AAFF);

        switch (currentTab) {
            case "STATS" -> renderStatsTab(guiGraphics, x, y);
            case "SKILLS" -> renderSkillsTab(guiGraphics, x, y);
            case "QUESTS" -> renderQuestTab(guiGraphics, x, y);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderStatsTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "§b§lSYSTEM: " + this.minecraft.player.getName().getString().toUpperCase(), x + 12, y + 10, 0xFFFFFF);        int pts = SystemData.getPoints(this.minecraft.player);
        g.drawString(this.font, "§fAvailable Points: §e" + pts, x + 15, y + 65, 0xFFFFFF);

        drawStat(g, "Strength:", SystemData.getStrength(this.minecraft.player), "§c", x + 15, y + 80);
        drawStat(g, "Health:", SystemData.getHealthStat(this.minecraft.player), "§a", x + 15, y + 100);
        drawStat(g, "Defense:", SystemData.getDefense(this.minecraft.player), "§7", x + 15, y + 120);
        drawStat(g, "Speed:", SystemData.getSpeed(this.minecraft.player), "§f", x + 15, y + 140);
        drawStat(g, "Mana:", SystemData.getMana(this.minecraft.player), "§d", x + 15, y + 160);
    }

    private void renderSkillsTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "§b§lSYSTEM: SKILLS", x + 12, y + 10, 0xFFFFFF);
        List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);

        int slotY = y + 40;
        int itemsToRender = 5; // Reduced from 6 to give more breathing room
        int spacing = 28;
        for (int i = skillScrollOffset; i < Math.min(skills.size(), skillScrollOffset + 5); i++) {
            int skillId = skills.get(i);
            String recipe = this.minecraft.player.getPersistentData().getString("manhwamod.skill_recipe_" + skillId);
            String displayName = SkillEngine.getSkillName(recipe);

            g.fill(x + 15, slotY, x + 175, slotY + 22, 0x44FFFFFF);
            g.drawString(this.font, "§e" + displayName, x + 20, slotY + 7, 0xFFFFFF);
            slotY += spacing;
        }

        if (skills.isEmpty()) {
            g.drawString(this.font, "§7No skills detected in bank.", x + 20, y + 50, 0xFFFFFF);
        } else {
            g.drawString(this.font, "§8(Scroll: " + (skillScrollOffset + 1) + "/" + Math.max(1, skills.size() - itemsToRender + 1) + ")", x + 50, y + WINDOW_HEIGHT - 40, 0x888888);
        }
        }
    }

    private void renderQuestTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "§b§lSYSTEM: DAILY QUEST", x + 12, y + 10, 0xFFFFFF);
        g.drawString(this.font, "§f- Pushups: §70/100", x + 15, y + 40, 0xFFFFFF);
    }

private void drawStat(GuiGraphics g, String label, int val, String color, int x, int y) {
    g.drawString(this.font, "§f" + label, x, y, 0xFFFFFF);
    g.drawString(this.font, color + val, x + 85, y, 0xFFFFFF);
}

@Override
public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (currentTab.equals("SKILLS")) {
        List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);
        // Business logic: Scroll through the bank
        if (delta < 0 && skillScrollOffset + 5 < skills.size()) skillScrollOffset++;
        if (delta > 0 && skillScrollOffset > 0) skillScrollOffset--;
        return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);

} // <--- THIS bracket must be the very last thing in the file.