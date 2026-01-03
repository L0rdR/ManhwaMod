package com.TaylorBros.ManhwaMod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public class EvaluationScreen extends Screen {
    private final LivingEntity target;

    public EvaluationScreen(LivingEntity target) {
        super(Component.literal("Evaluation"));
        this.target = target;
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Professional standard: Evaluation screens usually don't pause the game
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1. Darken the background slightly for readability
        this.renderBackground(graphics);

        int x = this.width / 2;
        int y = this.height / 2;

        // 2. Draw the "Analysis Frame" (A simple semi-transparent red box)
        graphics.fill(x - 80, y - 60, x + 80, y + 60, 0x88330000);
        graphics.renderOutline(x - 80, y - 70, 160, 140, 0xFFAAAAAA);
        // 3. Header
        graphics.drawCenteredString(this.font, "§c§l[ TARGET ANALYSIS ]", x, y - 50, 0xFFFFFFFF);

        // 4. Target Details
        graphics.drawString(this.font, "Name: §f" + target.getName().getString(), x - 70, y - 30, 0xFFFFFFFF);

        int level = target.getPersistentData().getInt("manhwamod.level");
        graphics.drawString(this.font, "Rank: §bLevel " + (level > 0 ? level : 1), x - 70, y - 15, 0xFFFFFFFF);

        int str = target.getPersistentData().getInt("manhwamod.strength");
        graphics.drawString(this.font, "Combat Power: §e" + (str > 0 ? str : "??"), x - 70, y, 0xFFFFFFFF);

        // 5. Health Bar Visualization
        float healthPct = target.getHealth() / target.getMaxHealth();
        graphics.drawString(this.font, "Vitality:", x - 70, y + 15, 0xFFFFFFFF);
        graphics.fill(x - 70, y + 25, x + 70, y + 30, 0xFF444444); // Bar Background
        graphics.fill(x - 70, y + 25, x - 70 + (int)(140 * healthPct), y + 30, 0xFF00FF00); // Green Health Bar

        graphics.drawCenteredString(this.font, "§7(Press ESC to close)", x, y + 45, 0xFFAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}