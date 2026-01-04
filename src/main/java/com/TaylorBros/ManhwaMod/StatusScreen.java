package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public class StatusScreen extends Screen {
    private static final int WINDOW_WIDTH = 190;
    private static final int WINDOW_HEIGHT = 210;
    private int multiplier = 1;
    private String currentTab = "STATS";
    private int skillScrollOffset = 0;

    protected StatusScreen() { super(Component.literal("System Status")); }

    @Override
    protected void init() {
        if (this.minecraft.player != null && !SystemData.isSystemPlayer(this.minecraft.player)) {
            this.minecraft.setScreen(new AwakenedStatusScreen());
            return;
        }

        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("STATS"), (button) -> { currentTab = "STATS"; this.rebuild(); }).bounds(x + 10, y + WINDOW_HEIGHT - 25, 55, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("SKILLS"), (button) -> { currentTab = "SKILLS"; this.rebuild(); }).bounds(x + 67, y + WINDOW_HEIGHT - 25, 55, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("QUESTS"), (button) -> { currentTab = "QUESTS"; this.rebuild(); }).bounds(x + 124, y + WINDOW_HEIGHT - 25, 55, 18).build());

        if (currentTab.equals("SKILLS")) {
            for (int s = 0; s < 5; s++) {
                int slotX = x + 15 + (s * 34); int slotY_Pos = y + 145; int finalS = s;
                this.addRenderableWidget(Button.builder(Component.literal(""), (b) -> { Messages.sendToServer(new PacketEquipSkill(finalS, 0)); this.rebuild(); }).bounds(slotX, slotY_Pos, 30, 30).build());
            }
        } else if (currentTab.equals("STATS")) {
            int buttonX = x + 160; int startY = y + 78;
            addStatButton(buttonX, startY, "STR"); addStatButton(buttonX, startY + 20, "HP"); addStatButton(buttonX, startY + 40, "DEF"); addStatButton(buttonX, startY + 60, "SPD"); addStatButton(buttonX, startY + 80, "MANA");
        }
    }

    private void rebuild() { this.clearWidgets(); this.init(); }

    private void addStatButton(int x, int y, String type) {
        this.addRenderableWidget(Button.builder(Component.literal("+"), (button) -> {
            int points = SystemData.getPoints(this.minecraft.player);
            int amount = Math.min(points, multiplier);
            if (amount > 0) { Messages.sendToServer(new PacketUpdateStats(amount, type)); this.rebuild(); }
        }).bounds(x, y, 20, 18).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;
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
        g.drawString(this.font, "§b§lSYSTEM: " + this.minecraft.player.getName().getString().toUpperCase(), x + 12, y + 10, 0xFFFFFF);
        int level = this.minecraft.player.getPersistentData().getInt("manhwamod.level");
        String rank = this.minecraft.player.getPersistentData().getString("manhwamod.rank");
        if (rank.isEmpty()) rank = "E";
        g.drawString(this.font, "§fRank: " + getRankColor(rank) + rank, x + 15, y + 25, 0xFFFFFF);
        g.drawString(this.font, "§fLevel: §b" + level + "§7/1000", x + 15, y + 35, 0xFFFFFF);
        g.fill(x + 15, y + 46, x + 175, y + 48, 0xFF444444);
        int levelWidth = (int)((level / 1000.0) * 160);
        g.fill(x + 15, y + 46, x + 15 + levelWidth, y + 48, 0xFF00AAFF);

        int pts = SystemData.getPoints(this.minecraft.player);
        int manaStat = SystemData.getMana(this.minecraft.player);
        g.drawString(this.font, "§fAvailable Points: §e" + pts, x + 15, y + 65, 0xFFFFFF);
        drawStat(g, "Strength:", SystemData.getStrength(this.minecraft.player), "§c", x + 15, y + 80);
        drawStat(g, "Health:", SystemData.getHealthStat(this.minecraft.player), "§a", x + 15, y + 100);
        drawStat(g, "Defense:", SystemData.getDefense(this.minecraft.player), "§7", x + 15, y + 120);
        drawStat(g, "Speed:", SystemData.getSpeed(this.minecraft.player), "§f", x + 15, y + 140);
        drawStat(g, "Mana:", manaStat, "§d", x + 15, y + 160);
        g.drawString(this.font, "§8Pool: " + SystemData.getCurrentMana(this.minecraft.player) + " / " + (manaStat * 10), x + 25, y + 172, 0xFFFFFF);
    }

    private String getRankColor(String rank) {
        return switch (rank) {
            case "SSS", "SS" -> "§6§l"; case "S" -> "§e§l"; case "A" -> "§c"; case "B" -> "§d"; default -> "§f";
        };
    }

    private void renderSkillsTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "§b§lSYSTEM: SKILLS", x + 12, y + 10, 0xFFFFFF);
        List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);
        int slotY = y + 40;
        for (int i = skillScrollOffset; i < Math.min(skills.size(), skillScrollOffset + 4); i++) {
            int skillId = skills.get(i);
            String name = SkillEngine.getSkillName(this.minecraft.player.getPersistentData().getString("manhwamod.skill_recipe_" + skillId));
            boolean isEquipped = false;
            for (int s = 0; s < 5; s++) if (this.minecraft.player.getPersistentData().getInt(SystemData.SLOT_PREFIX + s) == skillId) isEquipped = true;

            g.fill(x + 15, slotY, x + 175, slotY + 20, isEquipped ? 0x22888888 : 0x44FFFFFF);
            g.drawString(this.font, (isEquipped ? "§7" : "§e") + name, x + 20, slotY + 6, 0xFFFFFF);
            if (!isEquipped) {
                int finalId = skillId;
                this.addRenderableWidget(Button.builder(Component.literal("EQ"), (b) -> { equipToNextEmptySlot(finalId); this.rebuild(); }).bounds(x + 150, slotY + 1, 22, 18).build());
            }
            slotY += 22;
        }

        g.drawString(this.font, "§bEquipped Arts:", x + 15, y + 130, 0xFFFFFF);
        for (int s = 0; s < 5; s++) {
            int sX = x + 15 + (s * 34); int sY = y + 145;
            int id = this.minecraft.player.getPersistentData().getInt(SystemData.SLOT_PREFIX + s);
            g.fill(sX, sY, sX + 30, sY + 30, 0x66000000);
            g.renderOutline(sX, sY, 30, 30, 0xFF00AAFF);
            if (id != 0) {
                String sName = SkillEngine.getSkillName(this.minecraft.player.getPersistentData().getString("manhwamod.skill_recipe_" + id));
                g.pose().pushPose(); g.pose().translate(sX + 15, sY + 8, 0); g.pose().scale(0.6f, 0.6f, 1.0f);
                g.drawCenteredString(this.font, sName, 0, 5, 0xFFFFFF); g.pose().popPose();
            }
        }
    }

    private void equipToNextEmptySlot(int skillId) {
        for (int slot = 0; slot < 5; slot++) {
            int currentlyEquipped = this.minecraft.player.getPersistentData().getInt(SystemData.SLOT_PREFIX + slot);
            if (currentlyEquipped == skillId) return;
            if (currentlyEquipped == 0) { Messages.sendToServer(new PacketEquipSkill(slot, skillId)); return; }
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
            if (delta < 0 && skillScrollOffset + 4 < skills.size()) skillScrollOffset++;
            if (delta > 0 && skillScrollOffset > 0) skillScrollOffset--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}