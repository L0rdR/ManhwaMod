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
    private boolean showQuests = false; // Player exclusive tab

    protected StatusScreen() { super(Component.literal("System Status")); }

    @Override
    protected void init() {
        // TIERED ACCESS CHECK: If they aren't a 'Player', force them to the basic screen
        if (this.minecraft.player != null && !SystemData.isSystemPlayer(this.minecraft.player)) {
            this.minecraft.setScreen(new AwakenedStatusScreen());
            return;
        }

        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // TAB TOGGLE BUTTON
        this.addRenderableWidget(Button.builder(Component.literal(showQuests ? "BACK" : "DAILY QUESTS"), (button) -> {
            showQuests = !showQuests;
            this.clearWidgets();
            this.init();
        }).bounds(x + 10, y + WINDOW_HEIGHT - 25, 80, 18).build());

        if (!showQuests) {
            // Standard Stat Buttons (Similar to Awakened Screen)
            this.addRenderableWidget(Button.builder(Component.literal("x" + multiplier), (button) -> {
                multiplier = (multiplier == 1) ? 10 : (multiplier == 10) ? 100 : 1;
                button.setMessage(Component.literal("x" + multiplier));
            }).bounds(x + 135, y + 10, 45, 20).build());

            // ... (Stat Buttons logic here) ...
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // FIX: This prevents the pink/black checkerboard texture error
        this.renderBackground(guiGraphics);

        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // Draw Window
        guiGraphics.fill(x, y, x + WINDOW_WIDTH, y + WINDOW_HEIGHT, 0xAA000000);
        guiGraphics.renderOutline(x, y, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF00AAFF);

        if (showQuests) {
            renderQuestTab(guiGraphics, x, y);
        } else {
            renderStatsTab(guiGraphics, x, y);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderQuestTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "§b§lDAILY QUESTS", x + 12, y + 10, 0xFFFFFF);
        g.drawString(this.font, "§f- Pushups: §70/100", x + 15, y + 40, 0xFFFFFF);
        g.drawString(this.font, "§f- Running: §70/10km", x + 15, y + 55, 0xFFFFFF);
    }

    private void renderStatsTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "§b§lSYSTEM STATUS", x + 12, y + 10, 0xFFFFFF);
        int pts = SystemData.getPoints(this.minecraft.player);
        g.drawString(this.font, "§fPoints: §e" + pts, x + 15, y + 65, 0xFFFFFF);
        // ... (Remaining Stat drawings) ...
    }
}