package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class HunterPhoneScreen extends Screen {
    private int currentApp = 0;
    private int multiplier = 1;
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 5;
    private int selectedSkillId = -1;

    // --- SORTING STATE ---
    private enum SortMode {
        ID("ID"),
        RANK_DESC("Best"), // SSS -> F
        RANK_ASC("Worst"); // F -> SSS

        final String label;
        SortMode(String l) { this.label = l; }
    }
    private SortMode currentSort = SortMode.RANK_DESC; // Default to showing best skills first

    // COLORS
    private static final int COL_CASE_BG = 0xFF050510;
    private static final int COL_CASE_BORDER = 0xFF00AAFF;
    private static final int COL_SCREEN_BG = 0xAA001122;
    private static final int COL_TEXT_GLOW = 0xFF55FFFF;
    private static final int COL_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COL_BTN_NORMAL = 0x66000000;
    private static final int COL_BTN_HOVER = 0xFF0055AA;

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

        // HOME SCREEN
        if (currentApp == 0) {
            this.addRenderableWidget(new AppIcon(cx - 50, cy - 50, 0xFFCC0000, "STATUS", button -> switchApp(1)));
            this.addRenderableWidget(new AppIcon(cx + 10, cy - 50, 0xFF0055FF, "SKILLS", button -> switchApp(2)));
            if (SystemData.isSystemPlayer(player)) {
                this.addRenderableWidget(new AppIcon(cx - 50, cy + 10, 0xFFFFAA00, "QUEST", button -> switchApp(3)));
            }
            this.addRenderableWidget(new AppIcon(cx + 10, cy + 10, 0xFF00AA00, "MAP", button -> switchApp(4)));
        }

        // STATUS APP
        if (currentApp == 1) {
            int points = player.getPersistentData().getInt(SystemData.POINTS);
            if (points > 0) {
                int startY = cy - 40;
                int gap = 12;
                int buttonX = cx + 60;
                this.addRenderableWidget(Button.builder(Component.literal("x" + multiplier), b -> {
                    if (multiplier == 1) multiplier = 10;
                    else if (multiplier == 10) multiplier = 100;
                    else multiplier = 1;
                    b.setMessage(Component.literal("x" + multiplier));
                }).bounds(cx + 45, startY - 20, 30, 15).build());

                addButtonToGroup(createStatBtn(buttonX, startY, "strength"));
                addButtonToGroup(createStatBtn(buttonX, startY + gap, "agility"));
                addButtonToGroup(createStatBtn(buttonX, startY + gap*2, "vitality"));
                addButtonToGroup(createStatBtn(buttonX, startY + gap*3, "intelligence"));
                addButtonToGroup(createStatBtn(buttonX, startY + gap*4, "defense"));
            }
        }

        // SKILLS APP - Add Sort Button
        if (currentApp == 2) {
            // Tiny button in top-right corner of screen
            this.addRenderableWidget(Button.builder(Component.literal("Sort: " + currentSort.label), b -> {
                // Cycle: DESC -> ASC -> ID
                switch (currentSort) {
                    case RANK_DESC -> currentSort = SortMode.RANK_ASC;
                    case RANK_ASC -> currentSort = SortMode.ID;
                    case ID -> currentSort = SortMode.RANK_DESC;
                }
                b.setMessage(Component.literal("Sort: " + currentSort.label));
                // Reset scroll when sorting changes
                this.scrollOffset = 0;
            }).bounds(cx + 35, cy - 83, 45, 12).build());
        }

        // HOME BUTTON
        this.addRenderableWidget(Button.builder(Component.literal(""), button -> {
            if (currentApp == 0) this.onClose();
            else switchApp(0);
        }).bounds(cx - 20, cy + 98, 40, 8).build());
    }

    private Button createStatBtn(int x, int y, String stat) {
        return Button.builder(Component.literal("+"), b ->
                        Messages.sendToServer(new PacketIncreaseStat(stat, multiplier)))
                .bounds(x, y, 15, 10).build();
    }
    private void switchApp(int appId) {
        this.currentApp = appId;
        this.scrollOffset = 0;
        this.selectedSkillId = -1;
        this.init();
    }
    private void addButtonToGroup(Button b) { this.addRenderableWidget(b); this.statusButtons.add(b); }

    // --- HELPER TO GET SORTED LIST ---
    private List<Integer> getSortedSkills(Player p) {
        List<Integer> skills = new ArrayList<>(SystemData.getUnlockedSkills(p));

        if (currentSort == SortMode.ID) return skills; // Default order

        skills.sort((id1, id2) -> {
            String r1 = p.getPersistentData().getString(SystemData.RECIPE_PREFIX + id1);
            String r2 = p.getPersistentData().getString(SystemData.RECIPE_PREFIX + id2);
            SkillRanker.Rank rank1 = SkillRanker.getRank(r1);
            SkillRanker.Rank rank2 = SkillRanker.getRank(r2);

            // Compare ranks (High ordinal = Better Rank)
            if (currentSort == SortMode.RANK_DESC) {
                return Integer.compare(rank2.ordinal(), rank1.ordinal()); // SSS -> F
            } else {
                return Integer.compare(rank1.ordinal(), rank2.ordinal()); // F -> SSS
            }
        });
        return skills;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentApp == 2) {
            List<Integer> skills = getSortedSkills(this.minecraft.player);
            int maxOffset = Math.max(0, skills.size() - VISIBLE_ROWS);
            if (delta < 0) scrollOffset = Math.min(scrollOffset + 1, maxOffset);
            else if (delta > 0) scrollOffset = Math.max(scrollOffset - 1, 0);
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

        // Phone Body
        guiGraphics.fill(cx - 85, cy - 110, cx + 85, cy + 115, COL_CASE_BG);
        guiGraphics.renderOutline(cx - 85, cy - 110, 170, 225, COL_CASE_BORDER);
        guiGraphics.fill(cx - 80, cy - 90, cx + 80, cy + 90, COL_SCREEN_BG);
        guiGraphics.fill(cx - 30, cy - 110, cx + 30, cy - 100, 0xFF000000);

        guiGraphics.drawCenteredString(this.font, "12:00", cx, cy - 106, COL_TEXT_WHITE);
        guiGraphics.drawString(this.font, "100%", cx + 60, cy - 106, 0xFF00FF00);

        switch (currentApp) {
            case 0 -> renderHomeScreen(guiGraphics, cx, cy, p);
            case 1 -> renderStatusApp(guiGraphics, cx, cy, p);
            case 2 -> renderSkillsApp(guiGraphics, cx, cy, p, mouseX, mouseY);
            case 3 -> renderPlaceholderApp(guiGraphics, cx, cy, "DAILY QUEST");
            case 4 -> renderPlaceholderApp(guiGraphics, cx, cy, "DUNGEON MAP");
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderHomeScreen(GuiGraphics guiGraphics, int cx, int cy, Player p) {
        guiGraphics.drawCenteredString(this.font, "§bHUNTER PHONE", cx, cy - 75, COL_TEXT_WHITE);
        guiGraphics.drawCenteredString(this.font, "§7Welcome, " + p.getName().getString() + ".", cx, cy - 65, 0xFFAAAAAA);
    }

    private void renderStatusApp(GuiGraphics guiGraphics, int cx, int cy, Player p) {
        int str = p.getPersistentData().getInt(SystemData.STR);
        int agi = p.getPersistentData().getInt(SystemData.SPD);
        int vit = p.getPersistentData().getInt(SystemData.HP);
        int intel = p.getPersistentData().getInt(SystemData.MANA);
        int def = p.getPersistentData().getInt(SystemData.DEF);
        int points = p.getPersistentData().getInt(SystemData.POINTS);
        Affinity aff = SystemData.getAffinity(p);
        String rank = p.getPersistentData().getString("manhwamod.rank");
        if (rank.isEmpty()) rank = "E";

        guiGraphics.drawCenteredString(this.font, "§e" + p.getName().getString(), cx, cy - 80, COL_TEXT_WHITE);
        guiGraphics.drawCenteredString(this.font, "§7Rank: §b" + rank, cx, cy - 70, COL_TEXT_WHITE);
        guiGraphics.drawCenteredString(this.font, "Affinity: " + aff.color + aff.name, cx, cy - 62, 0xFFFFFFFF);

        int startY = cy - 40;
        int gap = 12;
        int textX = cx - 70;

        drawStatRow(guiGraphics, "Strength:", str, textX, startY, 0xFFFF5555);
        drawStatRow(guiGraphics, "Agility:", agi, textX, startY + gap, 0xFF55FF55);
        drawStatRow(guiGraphics, "Vitality:", vit, textX, startY + gap*2, 0xFFFFAA00);
        drawStatRow(guiGraphics, "Intellect:", intel, textX, startY + gap*3, 0xFF55FFFF);
        drawStatRow(guiGraphics, "Defense:", def, textX, startY + gap*4, 0xFF5555FF);
        guiGraphics.drawCenteredString(this.font, "§dPoints: " + points, cx, cy + 30, COL_TEXT_WHITE);
    }

    private void drawStatRow(GuiGraphics g, String label, int val, int x, int y, int color) {
        g.drawString(this.font, label, x, y, 0xFFAAAAAA);
        g.drawString(this.font, String.valueOf(val), x + 60, y, color);
    }

    private void renderSkillsApp(GuiGraphics guiGraphics, int cx, int cy, Player p, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "§3SKILL DATABASE", cx - 75, cy - 80, COL_TEXT_WHITE);

        // USE SORTED LIST HERE
        List<Integer> skills = getSortedSkills(p);

        if (skills.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "§7(No Arts Acquired)", cx, cy, 0x555555);
            return;
        }

        int startY = cy - 60;
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int dataIndex = i + scrollOffset;
            if (dataIndex >= skills.size()) break;

            int skillId = skills.get(dataIndex);
            int yPos = startY + (i * 25);

            String fullData = p.getPersistentData().getString(SystemData.RECIPE_PREFIX + skillId);
            int cost = p.getPersistentData().getInt(SystemData.COST_PREFIX + skillId);

            String displayName;
            if (fullData.contains("|")) {
                String[] split = fullData.split("\\|");
                displayName = split[1];
            } else {
                displayName = SkillEngine.getSkillName(fullData);
            }

            int nameColor = SkillRanker.getColor(fullData);
            SkillRanker.Rank rank = SkillRanker.getRank(fullData);
            String textToShow = "[" + rank.label + "] " + displayName;
            String costText = cost + " MP";

            boolean isHovered = (mouseX >= cx - 75 && mouseX <= cx + 75 && mouseY >= yPos && mouseY <= yPos + 22);
            boolean isSelected = (skillId == selectedSkillId);

            int bgColor = isSelected ? 0xFF004400 : (isHovered ? 0xFF002244 : 0x44000000);
            int outlineColor = isSelected ? 0xFF00FF00 : 0xFF000000;

            guiGraphics.fill(cx - 75, yPos, cx + 75, yPos + 22, bgColor);
            guiGraphics.renderOutline(cx - 75, yPos, 150, 22, outlineColor);

            int costWidth = this.font.width(costText);
            guiGraphics.drawString(this.font, costText, cx + 70 - costWidth, yPos + 7, COL_TEXT_GLOW);

            int availableWidth = 135 - costWidth;
            int nameWidth = this.font.width(textToShow);
            float scale = 0.8f;
            if (nameWidth * scale > availableWidth) scale = (float) availableWidth / nameWidth;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(cx - 70, yPos + 7, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(this.font, textToShow, 0, 0, nameColor);
            guiGraphics.pose().popPose();
        }

        int slotY = cy + 70;
        int slotSize = 20;
        int startX = cx - 70;
        int spacing = 30;
        guiGraphics.drawCenteredString(this.font, "§8[ Equip Slot ]", cx, slotY - 10, 0xFFAAAAAA);

        for (int i = 0; i < 5; i++) {
            int slotX = startX + (i * spacing);
            boolean hoverSlot = (mouseX >= slotX && mouseX <= slotX + slotSize && mouseY >= slotY && mouseY <= slotY + slotSize);
            int color = hoverSlot ? COL_BTN_HOVER : COL_BTN_NORMAL;
            if (selectedSkillId != -1 && hoverSlot) color = 0xFF00AA00;
            guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, color);
            guiGraphics.renderOutline(slotX, slotY, slotSize, slotSize, COL_CASE_BORDER);
            guiGraphics.drawCenteredString(this.font, String.valueOf(i + 1), slotX + 10, slotY + 6, COL_TEXT_WHITE);
        }
    }

    private void renderPlaceholderApp(GuiGraphics guiGraphics, int cx, int cy, String title) {
        guiGraphics.drawCenteredString(this.font, "§l" + title, cx, cy - 50, COL_TEXT_WHITE);
        guiGraphics.drawCenteredString(this.font, "Locked.", cx, cy, 0xFF555555);
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
            int renderColor = isHovered ? 0xFFFFFFFF : color;
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xDD000000);
            guiGraphics.renderOutline(getX(), getY(), width, height, renderColor);
            if (isHovered) {
                guiGraphics.drawCenteredString(Minecraft.getInstance().font, label, getX() + width / 2, getY() + height + 4, COL_TEXT_WHITE);
            } else {
                String letter = label.substring(0, 1);
                guiGraphics.drawCenteredString(Minecraft.getInstance().font, letter, getX() + width / 2, getY() + height / 2 - 4, renderColor);
            }
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (currentApp == 2) {
            int cx = this.width / 2;
            int cy = this.height / 2;
            int startY = cy - 60;
            List<Integer> skills = getSortedSkills(this.minecraft.player); // USE SORTED LIST FOR CLICKS TOO

            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int yPos = startY + (i * 25);
                if (mouseX >= cx - 75 && mouseX <= cx + 75 && mouseY >= yPos && mouseY <= yPos + 22) {
                    int dataIndex = i + scrollOffset;
                    if (dataIndex < skills.size()) {
                        this.selectedSkillId = skills.get(dataIndex);
                        this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 1.0f, 1.0f);
                        return true;
                    }
                }
            }

            // Click Equip Slot logic remains same...
            int slotY = cy + 70;
            int slotSize = 20;
            int startX = cx - 70;
            int spacing = 30;

            for (int i = 0; i < 5; i++) {
                int slotX = startX + (i * spacing);
                if (mouseX >= slotX && mouseX <= slotX + slotSize && mouseY >= slotY && mouseY <= slotY + slotSize) {
                    if (selectedSkillId != -1) {
                        Messages.sendToServer(new PacketEquipSkill(i, selectedSkillId));
                        this.minecraft.player.displayClientMessage(Component.literal("§a[System] Equipped to Slot " + (i + 1)), true);
                        this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        return true;
                    } else {
                        this.minecraft.player.displayClientMessage(Component.literal("§cSelect a Skill first."), true);
                    }
                }
            }
        }
        return false;
    }
}