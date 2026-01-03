package com.TaylorBros.ManhwaMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;

public class AwakenedStatusScreen extends Screen {
    private final List<Button> upgradeButtons = new ArrayList<>();
    private boolean isSkillTab = false;
    private int multiplier = 1; // Tracks x1, x10, x100
    private Button multiplierButton;

    public AwakenedStatusScreen() {
        super(Component.literal("Awakened Plate"));
    }

    @Override
    protected void init() {
        super.init();
        upgradeButtons.clear();
        int x = this.width / 2;
        int y = this.height / 2;

        // --- STAT BUTTONS ---
        addUpgradeButton(x + 55, y - 10, "MANA");
        addUpgradeButton(x + 55, y + 5, "STR");
        addUpgradeButton(x + 55, y + 20, "HP");
        addUpgradeButton(x + 55, y + 35, "DEF");
        addUpgradeButton(x + 55, y + 50, "SPD");

        // --- MULTIPLIER TOGGLE ---
        multiplierButton = this.addRenderableWidget(Button.builder(Component.literal("Amount: x" + multiplier), b -> {
            if (multiplier == 1) multiplier = 10;
            else if (multiplier == 10) multiplier = 100;
            else multiplier = 1;
            b.setMessage(Component.literal("Amount: x" + multiplier));
        }).bounds(x + 10, y + 68, 70, 14).build());

        // --- TAB TOGGLE ---
        this.addRenderableWidget(Button.builder(Component.literal("SKILLS"), b -> {
            this.isSkillTab = !this.isSkillTab;
            b.setMessage(this.isSkillTab ? Component.literal("STATS") : Component.literal("SKILLS"));
            multiplierButton.visible = !this.isSkillTab; // Hide multiplier when viewing skills
        }).bounds(x - 80, y + 68, 50, 14).build());
    }

    private void addUpgradeButton(int x, int y, String type) {
        upgradeButtons.add(this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            // Sends the current multiplier value (1, 10, or 100)
            Messages.sendToServer(new PacketUpdateStats(multiplier, type));
        }).bounds(x, y, 20, 14).build()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int x = this.width / 2;
        int y = this.height / 2;
        var player = Minecraft.getInstance().player;

        // Plate Background & Cyan Border
        graphics.fill(x - 85, y - 80, x + 85, y + 85, 0xDD222222);
        graphics.renderOutline(x - 85, y - 80, 170, 165, 0xFF00E5FF);

        if (player != null) {
            if (!isSkillTab) renderStatsPage(graphics, x, y, player);
            else renderSkillsPage(graphics, x, y, player);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderStatsPage(GuiGraphics graphics, int x, int y, Player player) {
        graphics.drawCenteredString(this.font, "§b§l[ AWAKENED STATUS ]", x, y - 70, 0xFFFFFFFF);
        int pts = SystemData.getPoints(player);

        // Buttons only show if player has enough points for the current multiplier
        for (Button btn : upgradeButtons) btn.visible = pts >= multiplier;

        graphics.drawString(this.font, "RANK: " + SystemData.getRank(player), x - 75, y - 55, 0xFFFFFFFF);
        graphics.drawString(this.font, "LVL:  §f" + player.getPersistentData().getInt("manhwamod.level"), x - 75, y - 43, 0xFFFFFFFF);
        graphics.drawString(this.font, "PTS:  §e" + pts, x - 75, y - 31, 0xFFFFFFFF);

        graphics.drawString(this.font, "MANA: §b" + SystemData.getMana(player), x - 75, y - 7, 0xFFFFFFFF);
        graphics.drawString(this.font, "STR:  §c" + SystemData.getStrength(player), x - 75, y + 8, 0xFFFFFFFF);
        graphics.drawString(this.font, "HP:   §a" + SystemData.getHealthStat(player), x - 75, y + 23, 0xFFFFFFFF);
        graphics.drawString(this.font, "DEF:  §9" + SystemData.getDefense(player), x - 75, y + 38, 0xFFFFFFFF);
        graphics.drawString(this.font, "SPD:  §e" + SystemData.getSpeed(player), x - 75, y + 53, 0xFFFFFFFF);
    }

    private void renderSkillsPage(GuiGraphics graphics, int x, int y, Player player) {
        graphics.drawCenteredString(this.font, "§6§l[ SYSTEM SKILLS ]", x, y - 70, 0xFFFFFFFF);
        for (Button btn : upgradeButtons) btn.visible = false;

        // Draw the 5 slots and library as you had before...
        // (Keeping your logic here for slot rendering)
    }
}