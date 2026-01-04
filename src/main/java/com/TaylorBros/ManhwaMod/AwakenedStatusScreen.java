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
    private int skillScrollOffset = 0;

    protected AwakenedStatusScreen() { super(Component.literal("Awakened Status")); }

    @Override
    protected void init() {
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // Toggle View Button
        this.addRenderableWidget(Button.builder(Component.literal(showSkills ? "VIEW STATS" : "VIEW SKILLS"), (button) -> {
            showSkills = !showSkills;
            this.rebuild();
        }).bounds(x + 10, y + WINDOW_HEIGHT - 25, 80, 18).build());

        if (!showSkills) {
            // Multiplier Button
            this.addRenderableWidget(Button.builder(Component.literal("x" + multiplier), (button) -> {
                multiplier = (multiplier == 1) ? 10 : (multiplier == 10) ? 100 : 1;
                this.rebuild();
            }).bounds(x + 135, y + 10, 45, 20).build());

            // Stat Increase Buttons
            int buttonX = x + 165;
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
            if (amount > 0) {
                Messages.sendToServer(new PacketUpdateStats(amount, type));
                this.rebuild();
            }
        }).bounds(x, y, 18, 18).build());
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
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
        g.drawString(this.font, "§b§lAWAKENED STATUS", x + 12, y + 10, 0xFFFFFF);

        int pts = SystemData.getPoints(this.minecraft.player);
        g.drawString(this.font, "§fPoints: §e" + pts, x + 15, y + 65, 0xFFFFFF);

        drawStat(g, "Strength:", SystemData.getStrength(this.minecraft.player), "§c", x + 15, y + 80);
        drawStat(g, "Health:", SystemData.getHealthStat(this.minecraft.player), "§a", x + 15, y + 100);
        drawStat(g, "Defense:", SystemData.getDefense(this.minecraft.player), "§7", x + 15, y + 120);
        drawStat(g, "Speed:", SystemData.getSpeed(this.minecraft.player), "§f", x + 15, y + 140);
        drawStat(g, "Mana:", SystemData.getMana(this.minecraft.player), "§d", x + 15, y + 160);

        g.drawString(this.font, "§8Pool: " + SystemData.getCurrentMana(this.minecraft.player) + " / " + (SystemData.getMana(this.minecraft.player) * 10), x + 25, y + 172, 0xFFFFFF);
    }

    private void renderSkillsTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "§b§lUNLOCKED ARTS", x + 12, y + 10, 0xFFFFFF);
        List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);
        int slotY = y + 35;

        for (int i = skillScrollOffset; i < Math.min(skills.size(), skillScrollOffset + 4); i++) {
            int skillId = skills.get(i);
            String name = SkillEngine.getSkillName(this.minecraft.player.getPersistentData().getString("manhwamod.skill_recipe_" + skillId));

            boolean equipped = false;
            for(int s=0; s<5; s++) if(this.minecraft.player.getPersistentData().getInt(SystemData.SLOT_PREFIX + s) == skillId) equipped = true;

            g.fill(x + 15, slotY, x + 175, slotY + 20, equipped ? 0x22888888 : 0x44FFFFFF);
            g.drawString(this.font, (equipped ? "§7" : "§e") + name, x + 20, slotY + 6, 0xFFFFFF);

            if (!equipped) {
                int finalId = skillId;
                this.addRenderableWidget(Button.builder(Component.literal("EQ"), (b) -> {
                    equipToNextEmptySlot(finalId);
                    this.rebuild();
                }).bounds(x + 150, slotY + 1, 22, 18).build());
            }
            slotY += 22;
        }

        g.drawString(this.font, "§bEquipped (Right-Click to Clear):", x + 15, y + 130, 0xFFFFFF);
        for (int s = 0; s < 5; s++) {
            int sX = x + 15 + (s * 34);
            int sY = y + 145;
            int id = this.minecraft.player.getPersistentData().getInt(SystemData.SLOT_PREFIX + s);

            g.fill(sX, sY, sX + 30, sY + 30, 0x66000000);
            g.renderOutline(sX, sY, 30, 30, 0xFF00AAFF);

            if (id != 0) {
                String sName = SkillEngine.getSkillName(this.minecraft.player.getPersistentData().getString("manhwamod.skill_recipe_" + id));
                g.pose().pushPose();
                g.pose().translate(sX + 15, sY + 8, 0);
                g.pose().scale(0.6f, 0.6f, 1.0f);
                if (sName.contains(" ")) {
                    String[] words = sName.split(" ", 2);
                    g.drawCenteredString(this.font, words[0], 0, 0, 0xFFFFFF);
                    g.drawCenteredString(this.font, words[1], 0, 10, 0xFFFFFF);
                } else {
                    g.drawCenteredString(this.font, sName, 0, 5, 0xFFFFFF);
                }
                g.pose().popPose();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // RIGHT CLICK (button 1) to unequip
        if (showSkills && button == 1) {
            for (int s = 0; s < 5; s++) {
                int sX = x + 15 + (s * 34);
                int sY = y + 145;
                if (mouseX >= sX && mouseX <= sX + 30 && mouseY >= sY && mouseY <= sY + 30) {
                    Messages.sendToServer(new PacketEquipSkill(s, 0));
                    this.rebuild();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void equipToNextEmptySlot(int skillId) {
        for (int slot = 0; slot < 5; slot++) {
            int currentlyEquipped = this.minecraft.player.getPersistentData().getInt(SystemData.SLOT_PREFIX + slot);
            if (currentlyEquipped == skillId) return;
            if (currentlyEquipped == 0) {
                Messages.sendToServer(new PacketEquipSkill(slot, skillId));
                return;
            }
        }
    }

    private void drawStat(GuiGraphics g, String label, int val, String color, int x, int y) {
        g.drawString(this.font, "§f" + label, x, y, 0xFFFFFF);
        g.drawString(this.font, color + val, x + 85, y, 0xFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showSkills) {
            List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);
            if (delta < 0 && skillScrollOffset + 4 < skills.size()) skillScrollOffset++;
            if (delta > 0 && skillScrollOffset > 0) skillScrollOffset--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}