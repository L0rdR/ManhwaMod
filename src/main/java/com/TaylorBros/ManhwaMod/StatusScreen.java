package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StatusScreen extends Screen {
    private static final int WINDOW_WIDTH = 190;
    private static final int WINDOW_HEIGHT = 210;
    private int multiplier = 1;

    protected StatusScreen() { super(Component.literal("Status Window")); }

    @Override
    protected void init() {
        // Cascade: Immediate redirect to Awakened UI if status changes
        if (this.minecraft.player != null && SystemData.isAwakened(this.minecraft.player)) {
            this.minecraft.setScreen(new AwakenedStatusScreen());
            return;
        }

        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("x" + multiplier), (button) -> {
            multiplier = (multiplier == 1) ? 10 : (multiplier == 10) ? 100 : 1;
            button.setMessage(Component.literal("x" + multiplier));
        }).bounds(x + 135, y + 10, 45, 20).build());

        // Stat Buttons Cascade
        int buttonX = x + 160;
        int startY = y + 78;
        String[] stats = {"STR", "HP", "DEF", "SPD", "MANA"};
        for (int i = 0; i < stats.length; i++) {
            addStatButton(buttonX, startY + (i * 20), stats[i]);
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

        // Cascade: Rendering with Centralized Constants
        int level = this.minecraft.player.getPersistentData().getInt(SystemData.LEVEL);
        int points = SystemData.getPoints(this.minecraft.player);

        guiGraphics.drawString(this.font, "§b§lSYSTEM STATUS", x + 12, y + 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Level: " + level, x + 15, y + 30, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Points: §e" + points, x + 15, y + 65, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}