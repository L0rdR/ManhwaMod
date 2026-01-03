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
        // Redirect if already awakened
        if (this.minecraft.player != null && SystemData.isAwakened(this.minecraft.player)) {
            this.minecraft.setScreen(new AwakenedStatusScreen());
            return;
        }

        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // MULTIPLIER BUTTON
        this.addRenderableWidget(Button.builder(Component.literal("x" + multiplier), (button) -> {
            multiplier = (multiplier == 1) ? 10 : (multiplier == 10) ? 100 : 1;
            button.setMessage(Component.literal("x" + multiplier));
        }).bounds(x + 135, y + 10, 45, 20).build());

        // STAT BUTTONS using centralized PacketUpdateStats
        int buttonX = x + 160;
        int startY = y + 78;
        addStatButton(buttonX, startY, "STR");
        addStatButton(buttonX, startY + 20, "HP");
        addStatButton(buttonX, startY + 40, "DEF");
        addStatButton(buttonX, startY + 60, "SPD");
        addStatButton(buttonX, startY + 80, "MANA");
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

        guiGraphics.fill(x, y, x + WINDOW_WIDTH, y + WINDOW_HEIGHT, 0xAA000000);
        guiGraphics.renderOutline(x, y, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF00AAFF);

        // Use Constants for rendering
        int level = this.minecraft.player.getPersistentData().getInt(SystemData.LEVEL);
        int pts = SystemData.getPoints(this.minecraft.player);

        guiGraphics.drawString(this.font, "§b§lSYSTEM STATUS", x + 12, y + 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "§fPoints: §e" + pts, x + 15, y + 65, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}