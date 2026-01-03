package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.List;

public class AwakenedStatusScreen extends Screen {
    private boolean isSkillTab = false;
    private int selectedSkillId = -1;
    private int multiplier = 1;

    public AwakenedStatusScreen() {
        super(Component.literal("Awakened Plate"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets(); // This removes old buttons when switching tabs

        int x = this.width / 2;
        int y = this.height / 2;

        // --- MULTIPLIER BUTTON (Stats only) ---
        if (!isSkillTab) {
            this.addRenderableWidget(Button.builder(Component.literal("Amt: x" + multiplier), b -> {
                multiplier = (multiplier == 1) ? 10 : (multiplier == 10) ? 100 : 1;
                b.setMessage(Component.literal("Amt: x" + multiplier));
            }).bounds(x + 5, y + 72, 75, 14).build());

            // Upgrade Buttons
            addStatBtn(x + 55, y - 42, "MANA");
            addStatBtn(x + 55, y - 22, "STR");
            addStatBtn(x + 55, y - 2, "HP");
            addStatBtn(x + 55, y + 18, "DEF");
            addStatBtn(x + 55, y + 38, "SPD");
        }

        // --- NAVIGATION ---
        this.addRenderableWidget(Button.builder(Component.literal("SWITCH VIEW"), b -> {
            this.isSkillTab = !this.isSkillTab;
            this.init();
        }).bounds(x - 80, y + 72, 75, 14).build());
    }

    private void addStatBtn(int x, int y, String type) {
        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            Messages.sendToServer(new PacketUpdateStats(multiplier, type));
        }).bounds(x, y, 14, 14).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int x = this.width / 2;
        int y = this.height / 2;
        Player player = this.minecraft.player;

        // Background Plate
        g.fill(x - 95, y - 90, x + 95, y + 100, 0xDD111111);
        g.renderOutline(x - 95, y - 90, 190, 190, 0xFF00E5FF);

        if (!isSkillTab) {
            renderStatsPage(g, x, y, player);
        } else {
            renderSkillManager(g, x, y, mouseX, mouseY, player);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderStatsPage(GuiGraphics g, int x, int y, Player player) {
        g.drawCenteredString(this.font, "§b[ SYSTEM STATS ]", x, y - 80, 0xFFFFFFFF);
        g.drawString(this.font, "Points: §e" + SystemData.getPoints(player), x - 85, y - 65, 0xFFFFFFFF);

        String[] labels = {"MANA", "STR", "HP", "DEF", "SPD"};
        int[] vals = {SystemData.getMana(player), SystemData.getStrength(player), SystemData.getHealthStat(player), SystemData.getDefense(player), SystemData.getSpeed(player)};

        for (int i = 0; i < labels.length; i++) {
            int rowY = y - 40 + (i * 20);
            g.drawString(this.font, labels[i] + ": §f" + vals[i], x - 85, rowY, 0xFFFFFFFF);
            g.fill(x - 85, rowY + 12, x + 50, rowY + 13, 0x33FFFFFF);
        }
    }

    private void renderSkillManager(GuiGraphics g, int x, int y, int mouseX, int mouseY, Player player) {
        g.drawCenteredString(this.font, "§6[ EQUIP SKILLS ]", x, y - 80, 0xFFFFFFFF);

        for (int i = 1; i <= 5; i++) {
            int slotY = y - 65 + (i * 16);
            int skillId = player.getPersistentData().getInt("manhwamod.slot_" + i);
            g.renderOutline(x - 85, slotY, 170, 14, 0xFF00E5FF);
            g.drawString(this.font, "Slot " + i + ": " + getSkillName(player, skillId), x - 81, slotY + 3, 0xFFFFFFFF);
        }

        g.drawCenteredString(this.font, "§a[ AVAILABLE BANK ]", x, y + 35, 0xFFFFFFFF);
        List<Integer> unlocked = SystemData.getUnlockedSkills(player);
        for (int i = 0; i < unlocked.size(); i++) {
            int id = unlocked.get(i);
            int bx = (x - 85) + (i % 2 * 87);
            int by = (y + 45) + (i / 2 * 16);
            int color = (id == selectedSkillId) ? 0xFF00FF00 : 0xFF555555;
            g.renderOutline(bx, by, 83, 14, color);
            g.drawString(this.font, getSkillName(player, id), bx + 4, by + 3, 0xFFAAAAAA);
        }
    }

    private String getSkillName(Player player, int skillId) {
        if (skillId == 0) return "§8- Empty -";
        String raw = player.getPersistentData().getString("manhwamod.skill_recipe_" + skillId);
        return raw.isEmpty() ? "Skill " + skillId : raw.split(":")[0];
    }
}