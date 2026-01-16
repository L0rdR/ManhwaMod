package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

public class HunterPhoneScreen extends Screen {
    private int currentApp = 0;
    private final List<Button> statusButtons = new ArrayList<>();

    public HunterPhoneScreen() { super(Component.literal("Hunter Phone")); }

    @Override
    protected void init() {
        this.clearWidgets();
        this.statusButtons.clear();
        int cx = this.width / 2;
        int cy = this.height / 2;
        Player player = this.minecraft.player;
        if (player == null) return;

        // --- 1. HOME SCREEN APPS ---
        if (currentApp == 0) {
            this.addRenderableWidget(new AppIcon(cx - 50, cy - 80, 0xFFFF5555, "STATUS", button -> switchApp(1)));
            this.addRenderableWidget(new AppIcon(cx + 10, cy - 80, 0xFFAA00AA, "SKILLS", button -> switchApp(2)));
            if (SystemData.isSystemPlayer(player)) {
                this.addRenderableWidget(new AppIcon(cx - 50, cy - 30, 0xFFFFAA00, "QUEST", button -> switchApp(3)));
            }
            this.addRenderableWidget(new AppIcon(cx + 10, cy - 30, 0xFF55FF55, "MAP", button -> switchApp(4)));
        }

        // --- 2. STATUS APP BUTTONS ---
        if (currentApp == 1) {
            int points = player.getPersistentData().getInt("manhwamod.stat_points");
            if (points > 0) {
                int startY = cy - 45;
                int gap = 12;

                // Strength
                addButtonToGroup(Button.builder(Component.literal("+"), b -> Messages.sendToServer(new PacketIncreaseStat("strength"))).bounds(cx + 20, startY, 15, 10).build());
                // Agility
                addButtonToGroup(Button.builder(Component.literal("+"), b -> Messages.sendToServer(new PacketIncreaseStat("agility"))).bounds(cx + 20, startY + gap, 15, 10).build());
                // Vitality
                addButtonToGroup(Button.builder(Component.literal("+"), b -> Messages.sendToServer(new PacketIncreaseStat("vitality"))).bounds(cx + 20, startY + gap*2, 15, 10).build());
                // Intelligence
                addButtonToGroup(Button.builder(Component.literal("+"), b -> Messages.sendToServer(new PacketIncreaseStat("intelligence"))).bounds(cx + 20, startY + gap*3, 15, 10).build());
                // Defense (NEW)
                addButtonToGroup(Button.builder(Component.literal("+"), b -> Messages.sendToServer(new PacketIncreaseStat("defense"))).bounds(cx + 20, startY + gap*4, 15, 10).build());
            }
        }

        // HOME BUTTON
        this.addRenderableWidget(Button.builder(Component.literal(""), button -> {
            if (currentApp == 0) this.onClose();
            else switchApp(0);
        }).bounds(cx - 15, cy + 130, 30, 10).build());
    }

    private void switchApp(int appId) { this.currentApp = appId; this.init(); }
    private void addButtonToGroup(Button b) { this.addRenderableWidget(b); this.statusButtons.add(b); }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int cx = this.width / 2;
        int cy = this.height / 2;
        Player p = this.minecraft.player;
        if (p == null) return;

        guiGraphics.fill(cx - 80, cy - 120, cx + 80, cy + 150, 0xFF101010);
        guiGraphics.fill(cx - 75, cy - 110, cx + 75, cy + 120, 0xFF202035);
        guiGraphics.fill(cx - 30, cy - 110, cx + 30, cy - 100, 0xFF000000);

        guiGraphics.drawCenteredString(this.font, "12:00", cx, cy - 108, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, "100%", cx + 50, cy - 108, 0xFF00FF00);

        switch (currentApp) {
            case 0 -> renderHomeScreen(guiGraphics, cx, cy);
            case 1 -> renderStatusApp(guiGraphics, cx, cy, p);
            case 2 -> renderSkillsApp(guiGraphics, cx, cy, p, mouseX, mouseY);
            case 3 -> renderPlaceholderApp(guiGraphics, cx, cy, "QUESTS");
            case 4 -> renderPlaceholderApp(guiGraphics, cx, cy, "MAP");
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderHomeScreen(GuiGraphics guiGraphics, int cx, int cy) {
        guiGraphics.drawCenteredString(this.font, "Hunter OS", cx, cy - 100, 0xAAAAAA);
    }

    private void renderStatusApp(GuiGraphics guiGraphics, int cx, int cy, Player p) {
        int str = p.getPersistentData().getInt(SystemData.STR);
        int agi = p.getPersistentData().getInt(SystemData.SPD);
        int vit = p.getPersistentData().getInt(SystemData.HP);
        int intel = p.getPersistentData().getInt("manhwamod.intelligence");
        int def = p.getPersistentData().getInt(SystemData.DEF); // NEW
        int points = p.getPersistentData().getInt("manhwamod.stat_points");

        guiGraphics.drawCenteredString(this.font, "§bAWAKENED PROFILE", cx, cy - 90, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "§e" + p.getName().getString(), cx, cy - 75, 0xFFFFFF);
        int startY = cy - 40;
        int gap = 12;

        guiGraphics.drawString(this.font, "Strength:   §c" + str, cx - 60, startY, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Agility:    §a" + agi, cx - 60, startY + gap, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Vitality:   §6" + vit, cx - 60, startY + gap*2, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Intelligence:      §b" + intel, cx - 60, startY + gap*3, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Defense:    §9" + def, cx - 60, startY + gap*4, 0xFFFFFF); // NEW

        guiGraphics.drawCenteredString(this.font, "§dPoints: " + points, cx, cy + 20, 0xFFFFFF);
    }

    // ... (Keep renderSkillsApp, renderPlaceholderApp, AppIcon class exactly as they were) ...
    // Since I must provide the full file or exact edits, assume the rest of the file (renderSkillsApp downwards) is unchanged from previous steps.
    // If you need the full renderSkillsApp again, let me know, otherwise just paste the AppIcon/etc logic here.
    private void renderSkillsApp(GuiGraphics guiGraphics, int cx, int cy, Player p, int mouseX, int mouseY) {
        // (Same as previous turn)
        guiGraphics.drawCenteredString(this.font, "§dACQUIRED ARTS", cx, cy - 90, 0xFFFFFF);
        int totalSkills = p.getPersistentData().getInt("manhwamod.total_unlocked");
        if (totalSkills <= 0) {
            guiGraphics.drawCenteredString(this.font, "§7No Arts Generated Yet.", cx, cy, 0x555555);
            return;
        }
        int startY = cy - 70;
        for (int i = 0; i < totalSkills; i++) {
            if (i > 5) break;
            int yPos = startY + (i * 25);
            String recipe = p.getPersistentData().getString("manhwamod.unlocked_skill_" + i);
            String displayName = SkillEngine.getSkillName(recipe);
            int cost = p.getPersistentData().getInt("manhwamod.unlocked_cost_" + i);
            boolean isHovered = (mouseX >= cx - 70 && mouseX <= cx + 70 && mouseY >= yPos && mouseY <= yPos + 22);
            guiGraphics.fill(cx - 70, yPos, cx + 70, yPos + 22, isHovered ? 0xFF303050 : 0xFF151520);
            guiGraphics.renderOutline(cx - 70, yPos, 140, 22, 0xFF000000);
            guiGraphics.drawString(this.font, displayName, cx - 65, yPos + 7, 0xFFFFFFFF);
            String costText = cost + " MP";
            guiGraphics.drawString(this.font, costText, cx + 65 - this.font.width(costText), yPos + 7, 0xFF55FFFF);
            if (isHovered) guiGraphics.drawCenteredString(this.font, "§e[Click to Equip]", cx, cy + 90, 0xFFFFFF);
        }
    }

    private void renderPlaceholderApp(GuiGraphics guiGraphics, int cx, int cy, String title) {
        guiGraphics.drawCenteredString(this.font, "§l" + title, cx, cy - 50, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Coming Soon...", cx, cy, 0x888888);
    }

    @Override public boolean isPauseScreen() { return false; }

    private class AppIcon extends Button {
        private final int color;
        private final String label;
        public AppIcon(int x, int y, int color, String label, OnPress onPress) {
            super(x, y, 32, 32, Component.empty(), onPress, DEFAULT_NARRATION);
            this.color = color;
            this.label = label;
        }
        @Override public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, color);
            if (isHovered) guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x44FFFFFF);
            guiGraphics.renderOutline(getX(), getY(), width, height, 0xFF000000);
            String symbol = label.substring(0, 1);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, symbol, getX() + width / 2, getY() + height / 2 - 4, 0xFFFFFFFF);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, label, getX() + width / 2, getY() + height + 2, 0xFFCCCCCC);
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (currentApp == 2) {
            int cx = this.width / 2;
            int cy = this.height / 2;
            int startY = cy - 70;
            int totalSkills = this.minecraft.player.getPersistentData().getInt("manhwamod.total_unlocked");
            for (int i = 0; i < totalSkills; i++) {
                if (i > 5) break;
                int yPos = startY + (i * 25);
                if (mouseX >= cx - 70 && mouseX <= cx + 70 && mouseY >= yPos && mouseY <= yPos + 22) {
                    Messages.sendToServer(new PacketEquipSkill(i, 0));
                    this.minecraft.player.displayClientMessage(Component.literal("§aEquipped to Slot 1!"), true);
                    return true;
                }
            }
        }
        return false;
    }
}