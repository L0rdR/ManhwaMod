package com.TaylorBros.ManhwaMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SystemQuestScreen extends Screen {
    public SystemQuestScreen() {
        super(Component.literal("Daily Quest"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int x = this.width / 2;
        int y = this.height / 2;

        // System Blue Aesthetic
        graphics.fill(x - 100, y - 80, x + 100, y + 80, 0xDD001122);
        graphics.renderOutline(x - 100, y - 80, 200, 160, 0xFF00AAFF);

        graphics.drawCenteredString(this.font, "§b§l[ DAILY QUEST: PREPARATION ]", x, y - 70, 0xFFFFFFFF);

        var player = Minecraft.getInstance().player;
        if (player != null) {
            boolean done = player.getPersistentData().getBoolean("manhwamod.quest_done");
            int kills = player.getPersistentData().getInt("manhwamod.quest_kills");
            int dist = (int) player.getPersistentData().getDouble("manhwamod.quest_dist");

            // Progress Bars / Text
            renderGoal(graphics, "Kills: ", kills, 50, x - 80, y - 30, 0xFF5555);
            renderGoal(graphics, "Sprint: ", dist, 2000, x - 80, y - 10, 0x55FF55);

            if (done) {
                graphics.drawCenteredString(this.font, "§6§lCOMPLETED", x, y + 40, 0xFFFFFFFF);
                graphics.drawCenteredString(this.font, "§7(Rewards claimed)", x, y + 50, 0xFFFFFFFF);
            } else {
                graphics.drawCenteredString(this.font, "§e§lIN PROGRESS", x, y + 40, 0xFFFFFFFF);
            }

            graphics.drawCenteredString(this.font, "§8Press 'ESC' to close", x, y + 70, 0xFF888888);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGoal(GuiGraphics g, String label, int current, int target, int x, int y, int color) {
        float progress = Math.min(1.0f, (float) current / target);
        g.drawString(this.font, label + current + "/" + target, x, y, 0xFFFFFFFF);
        // Background of bar
        g.fill(x, y + 10, x + 160, y + 14, 0xFF222222);
        // Progress of bar
        g.fill(x, y + 10, x + (int)(160 * progress), y + 14, color | 0xFF000000);
    }
}