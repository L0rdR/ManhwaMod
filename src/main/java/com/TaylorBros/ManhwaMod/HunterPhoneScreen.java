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
    private int multiplier = 1;

    // Scroll Offset for the Skills List
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 6;

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
            int points = player.getPersistentData().getInt(SystemData.POINTS);
            if (points > 0) {
                int startY = cy - 45;
                int gap = 12;
                int buttonX = cx + 60;

                // Multiplier Toggle
                this.addRenderableWidget(Button.builder(Component.literal("x" + multiplier), b -> {
                    if (multiplier == 1) multiplier = 10;
                    else if (multiplier == 10) multiplier = 100;
                    else multiplier = 1;
                    b.setMessage(Component.literal("x" + multiplier));
                }).bounds(cx + 45, startY - 20, 30, 15).build());

                // Stat Buttons
                addButtonToGroup(Button.builder(Component.literal("+"), b -> Messages.sendToServer(new PacketIncreaseStat("strength", multiplier))).bounds(buttonX, startY, 15, 10).build());
                addButtonToGroup(Button.builder(Component.literal("+"), b -> Messages.sendToServer(new PacketIncreaseStat("agility", multiplier))).bounds(buttonX, startY + gap, 15, 10).build());
                addButtonToGroup(Button.builder(Component.literal("+"), b -> Messages.sendToServer(new PacketIncreaseStat("vitality", multiplier))).bounds(buttonX, startY + gap*2, 15, 10).build());
                addButtonToGroup(Button.builder(Component.literal("+"), b -> Messages.sendToServer(new PacketIncreaseStat("intelligence", multiplier))).bounds(buttonX, startY + gap*3, 15, 10).build());
                addButtonToGroup(Button.builder(Component.literal("+"), b -> Messages.sendToServer(new PacketIncreaseStat("defense", multiplier))).bounds(buttonX, startY + gap*4, 15, 10).build());
            }
        }

        // HOME BUTTON
        this.addRenderableWidget(Button.builder(Component.literal(""), button -> {
            if (currentApp == 0) this.onClose();
            else switchApp(0);
        }).bounds(cx - 15, cy + 130, 30, 10).build());
    }

    private void switchApp(int appId) {
        this.currentApp = appId;
        this.scrollOffset = 0; // Reset scroll when switching apps
        this.init();
    }

    private void addButtonToGroup(Button b) { this.addRenderableWidget(b); this.statusButtons.add(b); }

    // --- HANDLE SCROLLING ---
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentApp == 2) { // Only scroll in Skills App
            List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);
            int maxOffset = Math.max(0, skills.size() - VISIBLE_ROWS);

            if (delta < 0) scrollOffset = Math.min(scrollOffset + 1, maxOffset); // Scroll Down
            else if (delta > 0) scrollOffset = Math.max(scrollOffset - 1, 0);   // Scroll Up

            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int cx = this.width / 2;
        int cy = this.height / 2;
        Player p = this.minecraft.player;
        if (p == null) return;

        // Draw Phone Body
        guiGraphics.fill(cx - 80, cy - 120, cx + 80, cy + 150, 0xFF101010);
        guiGraphics.fill(cx - 75, cy - 110, cx + 75, cy + 120, 0xFF202035);
        guiGraphics.fill(cx - 30, cy - 110, cx + 30, cy - 100, 0xFF000000);

        // Top Bar
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
        int intel = p.getPersistentData().getInt(SystemData.MANA);
        int def = p.getPersistentData().getInt(SystemData.DEF);
        int points = p.getPersistentData().getInt(SystemData.POINTS);

        guiGraphics.drawCenteredString(this.font, "§bAWAKENED PROFILE", cx, cy - 90, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "§e" + p.getName().getString(), cx, cy - 75, 0xFFFFFF);
        int startY = cy - 40;
        int gap = 12;
        int textX = cx - 70;

        guiGraphics.drawString(this.font, "Strength:   §c" + str, textX, startY, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Agility:    §a" + agi, textX, startY + gap, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Vitality:   §6" + vit, textX, startY + gap*2, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Intelligence: §b" + intel, textX, startY + gap*3, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Defense:    §9" + def, textX, startY + gap*4, 0xFFFFFF);

        guiGraphics.drawCenteredString(this.font, "§dPoints: " + points, cx, cy + 20, 0xFFFFFF);
    }

    // --- SCROLLABLE SKILLS LIST (SCALING FIX APPLIED) ---
    private void renderSkillsApp(GuiGraphics guiGraphics, int cx, int cy, Player p, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(this.font, "§dACQUIRED ARTS", cx, cy - 90, 0xFFFFFF);

        List<Integer> skills = SystemData.getUnlockedSkills(p);
        if (skills.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "§7No Arts Generated Yet.", cx, cy, 0x555555);
            return;
        }

        int startY = cy - 70;
        // Scroll Indicator
        String scrollInfo = (scrollOffset + 1) + "-" + Math.min(scrollOffset + VISIBLE_ROWS, skills.size()) + " of " + skills.size();
        guiGraphics.drawCenteredString(this.font, "§8" + scrollInfo, cx, cy + 105, 0xAAAAAA);

        // Render Loop
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            // Calculate actual index
            int dataIndex = i + scrollOffset;
            if (dataIndex >= skills.size()) break;

            int skillId = skills.get(dataIndex);
            int yPos = startY + (i * 25);

            String recipe = p.getPersistentData().getString(SystemData.RECIPE_PREFIX + skillId);
            int cost = p.getPersistentData().getInt(SystemData.COST_PREFIX + skillId);
            String displayName = SkillEngine.getSkillName(recipe);
            String costText = cost + " MP";

            boolean isHovered = (mouseX >= cx - 70 && mouseX <= cx + 70 && mouseY >= yPos && mouseY <= yPos + 22);

            // Draw Background
            guiGraphics.fill(cx - 70, yPos, cx + 70, yPos + 22, isHovered ? 0xFF303050 : 0xFF151520);
            guiGraphics.renderOutline(cx - 70, yPos, 140, 22, 0xFF000000);

            // Draw Cost (Right Aligned)
            int costWidth = this.font.width(costText);
            guiGraphics.drawString(this.font, costText, cx + 65 - costWidth, yPos + 7, 0xFF55FFFF);

            // --- SCALE NAME TO FIT ---
            // Available space = RightEdge(65) - CostWidth - LeftEdge(-65) - Padding(5)
            // Logic: Total 130 width. Name starts at left (-65). Cost is at right (+65).
            // Safe width = 125 - costWidth.
            int availableWidth = 125 - costWidth;
            int nameWidth = this.font.width(displayName);

            float scale = 0.8f; // Default small size for style
            if (nameWidth * scale > availableWidth) {
                // If even at 0.8x it's too big, shrink it further
                scale = (float) availableWidth / nameWidth;
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(cx - 65, yPos + 7, 0); // Move to start position
            guiGraphics.pose().scale(scale, scale, 1.0f);       // Apply shrinking
            guiGraphics.drawString(this.font, displayName, 0, 0, 0xFFFFFFFF);
            guiGraphics.pose().popPose();
            // -------------------------

            if (isHovered) {
                guiGraphics.drawCenteredString(this.font, "§e[Click to Equip]", cx, cy + 90, 0xFFFFFF);
            }
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

            List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);

            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int yPos = startY + (i * 25);
                if (mouseX >= cx - 70 && mouseX <= cx + 70 && mouseY >= yPos && mouseY <= yPos + 22) {

                    int dataIndex = i + scrollOffset;

                    if (dataIndex < skills.size()) {
                        int skillId = skills.get(dataIndex);
                        Messages.sendToServer(new PacketEquipSkill(0, skillId));

                        this.minecraft.player.displayClientMessage(Component.literal("§aEquipped to Slot 1!"), true);

                        // FIXED: Added .get() to unwrap the Reference<SoundEvent>
                        this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 1.0f, 1.0f);

                        return true;
                    }
                }
            }
        }
        return false;
    }
}