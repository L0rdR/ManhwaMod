package com.TaylorBros.ManhwaMod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class HunterPhoneScreen extends Screen {
    // --- COLORS ---
    private static final int COL_BG = 0xF5050510;
    private static final int COL_BORDER = 0xFF00AAFF;
    private static final int COL_NOTCH = 0xFF000000;
    private static final int COL_TEXT_TITLE = 0xFF00FFFF;
    private static final int COL_TEXT_BODY = 0xFFFFFFFF;

    // --- DIMENSIONS ---
    private static final int PHONE_W = 180;
    private static final int PHONE_H = 300;

    private int currentApp = 0;
    private int multiplier = 1;
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 5;
    private int selectedSkillId = -1;

    private float globalScale = 1.0f;
    private int scaledWidth, scaledHeight;

    private enum SortMode { ID("ID"), RANK_DESC("Best"), RANK_ASC("Worst"); final String label; SortMode(String l) { this.label = l; } }
    private SortMode currentSort = SortMode.RANK_DESC;

    private final List<Button> statusButtons = new ArrayList<>();

    public HunterPhoneScreen() { super(Component.literal("Hunter Phone")); }

    @Override
    protected void init() {
        this.clearWidgets();
        this.statusButtons.clear();

        // --- 1. CALCULATE SCALE ---
        float maxScaleH = (float)(this.height - 20) / PHONE_H;
        this.globalScale = Math.min(1.0f, maxScaleH);

        // --- 2. VIRTUAL COORDINATES ---
        this.scaledWidth = (int)(this.width / globalScale);
        this.scaledHeight = (int)(this.height / globalScale);

        int cx = scaledWidth / 2;
        int cy = scaledHeight / 2;

        Player player = this.minecraft.player;
        if (player == null) return;

        // --- 3. ADD WIDGETS ---

        // HOME SCREEN
        if (currentApp == 0) {
            this.addRenderableWidget(new AppIcon(cx - 65, cy - 50, getPlayerHead(player), "STATUS", b -> switchApp(1)));
            this.addRenderableWidget(new AppIcon(cx + 25, cy - 50, new ItemStack(Items.ENCHANTED_BOOK), "SKILLS", b -> switchApp(2)));
            this.addRenderableWidget(new AppIcon(cx - 65, cy + 30, new ItemStack(Items.EMERALD), "STORE", b -> switchApp(5)));
            this.addRenderableWidget(new AppIcon(cx + 25, cy + 30, new ItemStack(Items.COMPASS), "MAP", b -> switchApp(4)));

            if (SystemData.isSystemPlayer(player)) {
                this.addRenderableWidget(new AppIcon(cx - 20, cy - 10, new ItemStack(Items.WRITABLE_BOOK), "QUEST", b -> switchApp(3)));
            }
        }

        // STATUS APP
        if (currentApp == 1) {
            int points = player.getPersistentData().getInt(SystemData.POINTS);
            if (points > 0) {
                this.addRenderableWidget(Button.builder(Component.literal("x" + multiplier), b -> {
                    if (multiplier == 1) multiplier = 10; else if (multiplier == 10) multiplier = 100; else multiplier = 1;
                    b.setMessage(Component.literal("x" + multiplier));
                }).bounds(cx + 45, cy - 110, 30, 16).build());

                int startY = cy - 20;
                int buttonX = cx + 60;
                int gap = 18;

                addButtonToGroup(createStatBtn(buttonX, startY, "strength"));
                addButtonToGroup(createStatBtn(buttonX, startY + gap, "agility"));
                addButtonToGroup(createStatBtn(buttonX, startY + gap*2, "vitality"));
                addButtonToGroup(createStatBtn(buttonX, startY + gap*3, "intelligence"));
                addButtonToGroup(createStatBtn(buttonX, startY + gap*4, "defense"));
            }
        }

        // SKILLS APP
        if (currentApp == 2) {
            this.addRenderableWidget(Button.builder(Component.literal("Sort: " + currentSort.label), b -> {
                switch (currentSort) {
                    case RANK_DESC -> currentSort = SortMode.RANK_ASC;
                    case RANK_ASC -> currentSort = SortMode.ID;
                    case ID -> currentSort = SortMode.RANK_DESC;
                }
                b.setMessage(Component.literal("Sort: " + currentSort.label));
                this.scrollOffset = 0;
            }).bounds(cx + 35, cy - 75, 50, 16).build());

            // --- HIDE LOGIC START ---
            // Check if player has a BLANK Skill Orb
            boolean hasOrb = false;
            for(int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                // Check if item is Skill Orb AND has NO data (meaning it is blank)
                if (s.getItem() == ManhwaMod.SKILL_ORB.get() && !s.hasTag()) {
                    hasOrb = true;
                    break;
                }
            }

            // Only show button if they have the item
            if (hasOrb) {
                this.addRenderableWidget(Button.builder(Component.literal("CRYSTALLIZE"), b -> {
                    if (selectedSkillId != -1) {
                        Messages.sendToServer(new PacketExtractSkill(selectedSkillId));
                        selectedSkillId = -1;
                        this.init();
                    } else {
                        player.displayClientMessage(Component.literal("§cSelect a Skill first."), true);
                    }
                }).bounds(cx - 45, cy + 115, 90, 18).tooltip(Tooltip.create(Component.literal("Extract Skill to Orb"))).build());
            }
            // --- HIDE LOGIC END ---
        }

        // STORE APP
        if (currentApp == 5) {
            int btnX = cx - 60; int btnY = cy - 30;
            this.addRenderableWidget(Button.builder(Component.literal("Mystery Skill (10 Pts)"), b -> Messages.sendToServer(new PacketBuyItem(0))).bounds(btnX, btnY, 120, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Mana Elixir (5 Pts)"), b -> Messages.sendToServer(new PacketBuyItem(1))).bounds(btnX, btnY + 25, 120, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Gamble Box (1 Pt)"), b -> Messages.sendToServer(new PacketBuyItem(2))).bounds(btnX, btnY + 50, 120, 20).build());
        }

        // HOME BUTTON
        this.addRenderableWidget(Button.builder(Component.literal(""), button -> {
            if (currentApp == 0) this.onClose(); else switchApp(0);
        }).bounds(cx - 25, cy + 135, 50, 15).build());
    }

    // --- MOUSE INPUT TRANSFORMATION ---
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double virtualMX = mouseX / globalScale;
        double virtualMY = mouseY / globalScale;

        if (super.mouseClicked(virtualMX, virtualMY, button)) return true;

        if (currentApp == 2) {
            int cx = scaledWidth / 2;
            int cy = scaledHeight / 2;
            int startY = cy - 60;
            List<Integer> skills = getSortedSkills(this.minecraft.player);

            // Skill List
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int yPos = startY + (i * 28);
                if (virtualMX >= cx - 70 && virtualMX <= cx + 70 && virtualMY >= yPos && virtualMY <= yPos + 26) {
                    int dataIndex = i + scrollOffset;
                    if (dataIndex < skills.size()) {
                        this.selectedSkillId = skills.get(dataIndex);
                        this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 1.0f, 1.0f);
                        return true;
                    }
                }
            }
            // Equip Slots
            int slotY = cy + 90;
            int slotSize = 16;
            int spacing = 22;
            int startX = cx - 44;
            for (int i = 0; i < 5; i++) {
                int slotX = startX + (i * spacing);
                if (virtualMX >= slotX && virtualMX <= slotX + slotSize && virtualMY >= slotY && virtualMY <= slotY + slotSize) {
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

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(globalScale, globalScale, 1.0f);

        int vx = (int)(mouseX / globalScale);
        int vy = (int)(mouseY / globalScale);
        int cx = scaledWidth / 2;
        int cy = scaledHeight / 2;
        Player p = this.minecraft.player;

        if (p != null) {
            renderPhoneBody(guiGraphics, cx, cy, vx, vy);

            switch (currentApp) {
                case 0 -> renderHomeScreen(guiGraphics, cx, cy, p);
                case 1 -> renderStatusApp(guiGraphics, cx, cy, p, vx, vy);
                case 2 -> renderSkillsApp(guiGraphics, cx, cy, p, vx, vy);
                case 3 -> renderPlaceholderApp(guiGraphics, cx, cy, "DAILY QUEST");
                case 4 -> renderPlaceholderApp(guiGraphics, cx, cy, "DUNGEON MAP");
                case 5 -> renderStoreApp(guiGraphics, cx, cy, p);
            }
        }

        // Render Buttons manually
        for (net.minecraft.client.gui.components.events.GuiEventListener widget : this.children()) {
            if (widget instanceof net.minecraft.client.gui.components.Renderable r) {
                r.render(guiGraphics, vx, vy, partialTick);
            }
        }

        guiGraphics.pose().popPose();
    }

    // --- RENDER HELPERS ---
    private void renderPhoneBody(GuiGraphics g, int cx, int cy, int mx, int my) {
        g.fill(cx - 90, cy - 140, cx + 90, cy + 160, COL_BG);
        g.renderOutline(cx - 91, cy - 141, 182, 302, COL_BORDER);

        g.fill(cx - 90, cy - 140, cx + 90, cy - 120, COL_NOTCH);
        g.drawCenteredString(this.font, "12:00", cx, cy - 136, COL_TEXT_BODY);
        g.drawString(this.font, "100%", cx + 55, cy - 136, 0xFF00FF00);

        int hoverColor = (mx >= cx - 25 && mx <= cx + 25 && my >= cy + 135 && my <= cy + 150) ? COL_TEXT_TITLE : 0xFF555555;
        g.fill(cx - 20, cy + 140, cx + 20, cy + 142, hoverColor);
    }

    private void renderHomeScreen(GuiGraphics guiGraphics, int cx, int cy, Player p) {
        guiGraphics.drawCenteredString(this.font, "§bSYSTEM OS", cx, cy - 90, COL_TEXT_TITLE);
        guiGraphics.drawCenteredString(this.font, "Welcome, " + p.getName().getString(), cx, cy - 80, 0xFFAAAAAA);
    }

    private void renderStatusApp(GuiGraphics guiGraphics, int cx, int cy, Player p, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(this.font, "§bSTATUS", cx, cy - 110, COL_TEXT_TITLE);

        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, cx - 45, cy + 40, 40, (float)(cx - 45) - mouseX, (float)(cy + 40) - mouseY, p);
        } catch(Exception ignored) {}

        int str = p.getPersistentData().getInt(SystemData.STR);
        int agi = p.getPersistentData().getInt(SystemData.SPD);
        int vit = p.getPersistentData().getInt(SystemData.HP);
        int intel = p.getPersistentData().getInt(SystemData.MANA);
        int def = p.getPersistentData().getInt(SystemData.DEF);
        int points = p.getPersistentData().getInt(SystemData.POINTS);
        String rank = p.getPersistentData().getString("manhwamod.rank"); if (rank.isEmpty()) rank = "E";
        Affinity aff = SystemData.getAffinity(p);

        int startX = cx + 5; int startY = cy - 20; int gap = 18;

        guiGraphics.drawString(this.font, "Rank: §e" + rank, startX, startY - 30, COL_TEXT_BODY);
        guiGraphics.drawString(this.font, "Aff: " + aff.color + aff.name, startX, startY - 20, COL_TEXT_BODY);

        drawStatRow(guiGraphics, "STR:", str, startX, startY + 5, 0xFFFF5555);
        drawStatRow(guiGraphics, "AGI:", agi, startX, startY + gap + 5, 0xFF55FF55);
        drawStatRow(guiGraphics, "VIT:", vit, startX, startY + gap*2 + 5, 0xFFFFAA00);
        drawStatRow(guiGraphics, "INT:", intel, startX, startY + gap*3 + 5, 0xFF55FFFF);
        drawStatRow(guiGraphics, "DEF:", def, startX, startY + gap*4 + 5, 0xFF5555FF);

        guiGraphics.drawString(this.font, "Pts: §d" + points, cx - 15, cy + 80, COL_TEXT_BODY);
    }

    private void drawStatRow(GuiGraphics g, String label, int val, int x, int y, int color) { g.drawString(this.font, label, x, y, 0xFFAAAAAA); g.drawString(this.font, String.valueOf(val), x + 25, y, color); }

    private void renderSkillsApp(GuiGraphics guiGraphics, int cx, int cy, Player p, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(this.font, "§bSKILLS", cx, cy - 90, COL_TEXT_TITLE);
        List<Integer> skills = getSortedSkills(p);

        int startY = cy - 60;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int yPos = startY + (i * 28);
            guiGraphics.fill(cx - 70, yPos, cx + 70, yPos + 26, 0x44000000);
            guiGraphics.renderOutline(cx - 70, yPos, 140, 26, 0xFF003355);

            int dataIndex = i + scrollOffset;
            if (dataIndex >= skills.size()) continue;

            int skillId = skills.get(dataIndex);
            String fullData = p.getPersistentData().getString(SystemData.RECIPE_PREFIX + skillId);
            int cost = p.getPersistentData().getInt(SystemData.COST_PREFIX + skillId);
            String displayName = fullData.contains("|") ? fullData.split("\\|")[1] : SkillEngine.getSkillName(fullData);

            int nameColor = SkillRanker.getColor(fullData);
            SkillRanker.Rank rank = SkillRanker.getRank(fullData);

            boolean isHovered = (mouseX >= cx - 70 && mouseX <= cx + 70 && mouseY >= yPos && mouseY <= yPos + 26);
            boolean isSelected = (skillId == selectedSkillId);

            if (isSelected) guiGraphics.renderOutline(cx - 70, yPos, 140, 26, 0xFF00FF00);
            if (isHovered) guiGraphics.fill(cx - 70, yPos, cx + 70, yPos + 26, 0x22FFFFFF);

            guiGraphics.drawString(this.font, rank.label, cx - 65, yPos + 9, rank.color);

            // Scaled Name
            guiGraphics.pose().pushPose();
            float scale = 1.0f;
            int maxNameWidth = 70;
            if (this.font.width(displayName) > maxNameWidth) { scale = (float)maxNameWidth / this.font.width(displayName); }
            guiGraphics.pose().translate(cx - 45, yPos + 9, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(this.font, displayName, 0, 0, nameColor);
            guiGraphics.pose().popPose();

            String costStr = cost + " MP";
            guiGraphics.pose().pushPose();
            float costScale = 0.8f;
            guiGraphics.pose().translate(cx + 65 - (this.font.width(costStr)*costScale), yPos + 10, 0);
            guiGraphics.pose().scale(costScale, costScale, 1.0f);
            guiGraphics.drawString(this.font, costStr, 0, 0, 0xFF55FFFF);
            guiGraphics.pose().popPose();
        }

        int slotY = cy + 90;
        guiGraphics.drawCenteredString(this.font, "§8[ Equip Slot ]", cx, slotY - 10, 0xFFAAAAAA);
        int slotSize = 16;
        int spacing = 22;
        int startX = cx - 44;

        for (int i = 0; i < 5; i++) {
            int slotX = startX + (i * spacing);
            boolean hoverSlot = (mouseX >= slotX && mouseX <= slotX + slotSize && mouseY >= slotY && mouseY <= slotY + slotSize);
            int color = hoverSlot ? 0xFF00AAFF : 0x66000000;
            if (selectedSkillId != -1 && hoverSlot) color = 0xFF00FF00;

            guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, color);
            guiGraphics.renderOutline(slotX, slotY, slotSize, slotSize, COL_BORDER);
            guiGraphics.drawCenteredString(this.font, String.valueOf(i + 1), slotX + 8, slotY + 4, COL_TEXT_BODY);
        }
    }

    private void renderStoreApp(GuiGraphics guiGraphics, int cx, int cy, Player p) {
        guiGraphics.drawCenteredString(this.font, "§dSTORE", cx, cy - 80, COL_TEXT_TITLE);
        guiGraphics.drawCenteredString(this.font, "Balance: §e" + SystemData.getPoints(p) + " Pts", cx, cy - 65, COL_TEXT_BODY);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(cx, cy + 20, 100);
        guiGraphics.pose().scale(3.0f, 3.0f, 3.0f);
        guiGraphics.pose().popPose();
    }

    private void renderPlaceholderApp(GuiGraphics guiGraphics, int cx, int cy, String title) {
        guiGraphics.drawCenteredString(this.font, "§l" + title, cx, cy - 20, COL_TEXT_TITLE);
        guiGraphics.drawCenteredString(this.font, "LOCKED", cx, cy, 0xFF555555);
    }

    private ItemStack getPlayerHead(Player player) { ItemStack head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", player.getName().getString()); return head; }
    private Button createStatBtn(int x, int y, String stat) { return Button.builder(Component.literal("+"), b -> Messages.sendToServer(new PacketIncreaseStat(stat, multiplier))).bounds(x, y, 14, 14).build(); }
    private void switchApp(int appId) { this.currentApp = appId; this.scrollOffset = 0; this.selectedSkillId = -1; this.init(); }
    private void addButtonToGroup(Button b) { this.addRenderableWidget(b); this.statusButtons.add(b); }
    private List<Integer> getSortedSkills(Player p) {
        List<Integer> skills = new ArrayList<>(SystemData.getUnlockedSkills(p));
        if (currentSort == SortMode.ID) return skills;
        skills.sort((id1, id2) -> {
            String r1 = p.getPersistentData().getString(SystemData.RECIPE_PREFIX + id1);
            String r2 = p.getPersistentData().getString(SystemData.RECIPE_PREFIX + id2);
            SkillRanker.Rank rank1 = SkillRanker.getRank(r1);
            SkillRanker.Rank rank2 = SkillRanker.getRank(r2);
            return (currentSort == SortMode.RANK_DESC) ? Integer.compare(rank2.ordinal(), rank1.ordinal()) : Integer.compare(rank1.ordinal(), rank2.ordinal());
        });
        return skills;
    }

    private class AppIcon extends Button {
        private final ItemStack iconItem;
        private final String label;
        public AppIcon(int x, int y, ItemStack iconItem, String label, OnPress onPress) {
            super(x, y, 40, 48, Component.empty(), onPress, DEFAULT_NARRATION);
            this.iconItem = iconItem;
            this.label = label;
        }
        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int borderCol = isHovered ? COL_TEXT_TITLE : 0xFF004488;
            int bgCol = isHovered ? 0x4400FFFF : 0x22000000;
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgCol);
            guiGraphics.renderOutline(getX(), getY(), width, height, borderCol);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(getX() + 12, getY() + 8, 0);
            guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
            guiGraphics.renderItem(iconItem, -4, -4);
            guiGraphics.pose().popPose();

            int textColor = isHovered ? COL_TEXT_TITLE : 0xFFAAAAAA;
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, label, getX() + width / 2, getY() + height - 10, textColor);
        }
    }
}