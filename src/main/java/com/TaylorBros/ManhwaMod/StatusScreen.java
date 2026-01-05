package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public class StatusScreen extends Screen {
    private static final int WINDOW_WIDTH = 190;
    private static final int WINDOW_HEIGHT = 210;
    private int multiplier = 1;
    private String currentTab = "STATS";
    private int skillScrollOffset = 0;

    protected StatusScreen() { super(Component.literal("System Status")); }

    @Override
    protected void init() {
        super.init();

        if (this.minecraft.player != null && !SystemData.isSystemPlayer(this.minecraft.player)) {
            this.minecraft.setScreen(new AwakenedStatusScreen());
            return;
        }

        // Standardized coordinates for the entire class
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        // Navigation Buttons
        this.addRenderableWidget(Button.builder(Component.literal("STATS"), (button) -> { currentTab = "STATS"; this.rebuild(); })
                .bounds(x + 10, y + WINDOW_HEIGHT - 25, 55, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("SKILLS"), (button) -> { currentTab = "SKILLS"; this.rebuild(); })
                .bounds(x + 67, y + WINDOW_HEIGHT - 25, 55, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("QUESTS"), (button) -> { currentTab = "QUESTS"; this.rebuild(); })
                .bounds(x + 124, y + WINDOW_HEIGHT - 25, 55, 18).build());

        // Stats Tab Specific Buttons
        if (currentTab.equals("STATS")) {
            this.addRenderableWidget(Button.builder(Component.literal("x" + multiplier), (button) -> {
                multiplier = (multiplier == 1) ? 10 : (multiplier == 10) ? 100 : 1;
                button.setMessage(Component.literal("x" + multiplier));
            }).bounds(x + 135, y + 10, 45, 20).build());

            int buttonX = x + 160;
            int startStatY = y + 78;
            addStatButton(buttonX, startStatY, "STR");
            addStatButton(buttonX, startStatY + 20, "HP");
            addStatButton(buttonX, startStatY + 40, "DEF");
            addStatButton(buttonX, startStatY + 60, "SPD");
            addStatButton(buttonX, startStatY + 80, "MANA");
        }
    }

    private void rebuild() { this.clearWidgets(); this.init(); }

    private void addStatButton(int x, int y, String type) {
        this.addRenderableWidget(Button.builder(Component.literal("+"), (button) -> {
            int points = SystemData.getPoints(this.minecraft.player);
            int amount = Math.min(points, multiplier);
            if (amount > 0) Messages.sendToServer(new PacketUpdateStats(amount, type));
        }).bounds(x, y, 20, 18).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        guiGraphics.fill(x, y, x + WINDOW_WIDTH, y + WINDOW_HEIGHT, 0xAA000000);
        guiGraphics.renderOutline(x, y, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF00AAFF);

        switch (currentTab) {
            case "STATS" -> renderStatsTab(guiGraphics, x, y);
            case "SKILLS" -> renderSkillsTab(guiGraphics, x, y, mouseX, mouseY);
            case "QUESTS" -> renderQuestsTab(guiGraphics, x, y); // Matches method name below
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderStatsTab(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "§b§lSYSTEM: " + this.minecraft.player.getName().getString().toUpperCase(), x + 12, y + 10, 0xFFFFFF);
        int pts = SystemData.getPoints(this.minecraft.player);
        int manaStat = SystemData.getMana(this.minecraft.player);
        int currentMana = this.minecraft.player.getPersistentData().getInt(SystemData.CURRENT_MANA);

        g.drawString(this.font, "§fAvailable Points: §e" + pts, x + 15, y + 65, 0xFFFFFF);
        drawStat(g, "Strength:", SystemData.getStrength(this.minecraft.player), "§c", x + 15, y + 80);
        drawStat(g, "Health:", SystemData.getHealthStat(this.minecraft.player), "§a", x + 15, y + 100);
        drawStat(g, "Defense:", SystemData.getDefense(this.minecraft.player), "§7", x + 15, y + 120);
        drawStat(g, "Speed:", SystemData.getSpeed(this.minecraft.player), "§f", x + 15, y + 140);

        int level = this.minecraft.player.getPersistentData().getInt(SystemData.LEVEL);
        String rank = this.minecraft.player.getPersistentData().getString("manhwamod.rank");
        if (rank.isEmpty()) rank = "E";

        g.drawString(this.font, "§fRank: " + getRankColor(rank) + rank, x + 15, y + 25, 0xFFFFFF);
        g.drawString(this.font, "§fLevel: §b" + level + "§7/1000", x + 15, y + 35, 0xFFFFFF);

        g.fill(x + 15, y + 46, x + 175, y + 48, 0xFF444444);
        int levelWidth = (int)((level / 1000.0) * 160);
        g.fill(x + 15, y + 46, x + 15 + levelWidth, y + 48, 0xFF00AAFF);

        drawStat(g, "Mana:", manaStat, "§d", x + 15, y + 160);
        g.drawString(this.font, "§8Pool: " + currentMana + " / " + (manaStat * 10), x + 25, y + 172, 0xFFFFFF);
    }

    private String getRankColor(String rank) {
        return switch (rank) {
            case "SSS", "SS" -> "§6§l";
            case "S" -> "§e§l";
            case "A" -> "§c";
            case "B" -> "§d";
            default -> "§f";
        };
    }

    public void renderSkillsTab(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        g.drawString(this.font, "§b§lUNLOCKED ARTS", x + 12, y + 10, 0xFFFFFF);
        List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);
        int slotY = y + 35;

        for (int i = skillScrollOffset; i < Math.min(skills.size(), skillScrollOffset + 4); i++) {
            int skillId = skills.get(i);
            String recipe = this.minecraft.player.getPersistentData().getString(SystemData.RECIPE_PREFIX + skillId);
            String name = SkillEngine.getSkillName(recipe);

            boolean isEquipped = false;
            for (int checkSlot = 0; checkSlot < 5; checkSlot++) {
                if (this.minecraft.player.getPersistentData().getInt(SystemData.SLOT_PREFIX + checkSlot) == skillId) {
                    isEquipped = true;
                    break;
                }
            }

            g.fill(x + 15, slotY, x + 175, slotY + 20, isEquipped ? 0x22888888 : 0x44FFFFFF);
            g.drawString(this.font, (isEquipped ? "§7" : "§e") + name, x + 20, slotY + 6, 0xFFFFFF);

            if (!isEquipped) {
                g.fill(x + 150, slotY + 1, x + 172, slotY + 19, 0x6600AAFF);
                g.drawCenteredString(this.font, "EQ", x + 161, slotY + 6, 0xFFFFFF);
            } else {
                g.drawString(this.font, "§a✔", x + 155, slotY + 6, 0xFFFFFF);
            }
            slotY += 22;
        }

        g.drawString(this.font, "§bEquipped Arts:", x + 15, y + 130, 0xFFFFFF);
        for (int slotIdx = 0; slotIdx < 5; slotIdx++) {
            int slotX = x + 15 + (slotIdx * 34);
            int slotY_Pos = y + 145;
            int equippedId = this.minecraft.player.getPersistentData().getInt(SystemData.SLOT_PREFIX + slotIdx);

            g.fill(slotX, slotY_Pos, slotX + 30, slotY_Pos + 30, 0x66000000);
            g.renderOutline(slotX, slotY_Pos, 30, 30, 0xFF00AAFF);

            if (equippedId != 0) {
                String recipe = this.minecraft.player.getPersistentData().getString(SystemData.RECIPE_PREFIX + equippedId);
                String skillName = SkillEngine.getSkillName(recipe);

                g.pose().pushPose();
                float scale = skillName.length() > 6 ? 0.55f : 0.75f;
                g.pose().translate(slotX + 15, slotY_Pos + 15, 0);
                g.pose().scale(scale, scale, 1.0f);
                g.drawCenteredString(this.font, skillName, 0, -4, 0xFFFFFF);
                g.pose().popPose();

                if (mouseX >= slotX && mouseX <= slotX + 30 && mouseY >= slotY_Pos && mouseY <= slotY_Pos + 30) {
                    g.fill(slotX, slotY_Pos, slotX + 30, slotY_Pos + 30, 0x66FF0000);
                    g.drawCenteredString(this.font, "CLR", slotX + 15, slotY_Pos + 10, 0xFFFFFF);
                }
            } else {
                g.drawString(this.font, "§8" + (slotIdx + 1), slotX + 12, slotY_Pos + 10, 0xFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentTab.equals("SKILLS") && button == 0) {
            int x = (this.width - WINDOW_WIDTH) / 2;
            int y = (this.height - WINDOW_HEIGHT) / 2;

            List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);
            int listY = y + 35;
            for (int i = skillScrollOffset; i < Math.min(skills.size(), skillScrollOffset + 4); i++) {
                int rowY = listY + (i - skillScrollOffset) * 22;
                if (mouseX >= x + 150 && mouseX <= x + 172 && mouseY >= rowY + 1 && mouseY <= rowY + 19) {
                    equipToNextEmptySlot(skills.get(i));
                    return true;
                }
            }

            for (int slot = 0; slot < 5; slot++) {
                int sx = x + 15 + (slot * 34);
                int sy = y + 145;
                if (mouseX >= sx && mouseX <= sx + 30 && mouseY >= sy && mouseY <= sy + 30) {
                    Messages.sendToServer(new PacketEquipSkill(slot, 0));
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

    private void renderQuestsTab(GuiGraphics guiGraphics, int x, int y) {
        var player = this.minecraft.player;
        if (player == null) return;

        // Pulling data from DailyQuestData
        int kills = DailyQuestData.getKills(player);
        int dist = (int) DailyQuestData.getDist(player);
        boolean completed = DailyQuestData.isCompleted(player);

        guiGraphics.drawString(this.font, "§lDAILY QUEST", x + 12, y + 10, 0xFFFFFF);

        String killProgress = "Mobs Defeated: " + kills + " / " + DailyQuestData.getKillTarget(player);
        int killColor = kills >= DailyQuestData.getKillTarget(player) ? 0x55FF55 : 0xAAAAAA;

        String distProgress = "Running: " + dist + " / " + DailyQuestData.getDistTarget(player) + "m";
        int distColor = dist >= DailyQuestData.getDistTarget(player) ? 0x55FF55 : 0xAAAAAA;

        if (completed) {
            guiGraphics.drawString(this.font, "§6§lQUEST COMPLETED", x + 12, y + 75, 0xFFAA00);
            guiGraphics.drawString(this.font, "§e+3 Ability Points Rewarded", x + 12, y + 87, 0xFFFF55);
        } else {
            guiGraphics.drawString(this.font, "§fStatus: §bIn Progress...", x + 12, y + 75, 0xFFFFFF);
        }
    }

    private void drawStat(GuiGraphics g, String label, int val, String color, int x, int y) {
        g.drawString(this.font, "§f" + label, x, y, 0xFFFFFF);
        g.drawString(this.font, color + val, x + 85, y, 0xFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentTab.equals("SKILLS")) {
            List<Integer> skills = SystemData.getUnlockedSkills(this.minecraft.player);
            if (delta < 0 && skillScrollOffset + 4 < skills.size()) skillScrollOffset++;
            if (delta > 0 && skillScrollOffset > 0) skillScrollOffset--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}