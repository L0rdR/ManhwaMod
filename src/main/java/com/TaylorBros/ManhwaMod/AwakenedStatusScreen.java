package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import java.util.List;

public class AwakenedStatusScreen extends Screen {
    private boolean isSkillTab = false;
    private int selectedSkillId = -1;
    private int multiplier = 1;
    private int scrollOffset = 0; // Tracks the starting row for the bank

    public AwakenedStatusScreen() {
        super(Component.literal("Awakened Plate"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        int x = this.width / 2;
        int y = this.height / 2;

        // Move Navigation way down to y+105 to make room for 20 skills
        this.addRenderableWidget(Button.builder(Component.literal("SWITCH VIEW"), b -> {
            this.isSkillTab = !this.isSkillTab;
            this.init();
        }).bounds(x - 80, y + 105, 75, 14).build());

        if (!isSkillTab) {
            this.addRenderableWidget(Button.builder(Component.literal("Amt: x" + multiplier), b -> {
                multiplier = (multiplier == 1) ? 10 : (multiplier == 10) ? 100 : 1;
                b.setMessage(Component.literal("Amt: x" + multiplier));
            }).bounds(x + 5, y + 105, 75, 14).build());

            addStatBtn(x + 55, y - 42, "MANA");
            addStatBtn(x + 55, y - 22, "STR");
            addStatBtn(x + 55, y - 2, "HP");
            addStatBtn(x + 55, y + 18, "DEF");
            addStatBtn(x + 55, y + 38, "SPD");
        }
    }

    private void addStatBtn(int x, int y, String type) {
        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            Messages.sendToServer(new PacketUpdateStats(multiplier, type));
        }).bounds(x, y, 14, 14).build());
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isSkillTab) {
            if (delta > 0 && scrollOffset > 0) {
                scrollOffset--; // Scroll Up
            } else if (delta < 0) {
                List<Integer> unlocked = SystemData.getUnlockedSkills(this.minecraft.player);
                // Check if there is another row to scroll down to
                if ((scrollOffset + 3) * 2 < unlocked.size()) {
                    scrollOffset++; // Scroll Down
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isSkillTab) return super.mouseClicked(mouseX, mouseY, button);
        int x = this.width / 2;
        int y = this.height / 2;
        Player player = this.minecraft.player;

        // 1. BANK SELECTION (Bottom half)
        List<Integer> unlocked = SystemData.getUnlockedSkills(player);
        for (int i = 0; i < 6; i++) { // Only check the 6 visible boxes
            int index = (scrollOffset * 2) + i;
            if (index >= unlocked.size()) break;

            int bx = (x - 85) + (i % 2 * 87);
            int by = (y + 35) + (i / 2 * 16);

            if (mouseX >= bx && mouseX <= bx + 83 && mouseY >= by && mouseY <= by + 14) {
                this.selectedSkillId = unlocked.get(index);
                player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 1.0f, 1.2f);
                return true;
            }
        }

        // 2. SLOT EQUIPPING (Top half)
        for (int i = 1; i <= 5; i++) {
            int slotY = y - 65 + (i * 15);
            if (mouseX >= x - 85 && mouseX <= x + 85 && mouseY >= slotY && mouseY <= slotY + 14) {
                if (selectedSkillId != -1) {
                    Messages.sendToServer(new PacketEquipSkill(i, selectedSkillId));
                    player.playSound(SoundEvents.ARMOR_EQUIP_ELYTRA, 1.0f, 1.0f);
                    this.selectedSkillId = -1;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int x = this.width / 2;
        int y = this.height / 2;
        Player player = this.minecraft.player;

        // Expanded Plate: 220 pixels tall to fit all elements
        g.fill(x - 95, y - 90, x + 95, y + 125, 0xDD111111);
        g.renderOutline(x - 95, y - 90, 190, 215, 0xFF00E5FF);

        if (!isSkillTab) {
            renderStatsPage(g, x, y, player);
        } else {
            renderSkillManager(g, x, y, player);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderSkillManager(GuiGraphics g, int x, int y, Player player) {
        g.drawCenteredString(this.font, "§6[ EQUIP SKILLS ]", x, y - 80, 0xFFFFFFFF);

        // 1. ACTIVE SLOTS (Top - Fixed)
        for (int i = 1; i <= 5; i++) {
            int slotY = y - 65 + (i * 15);
            int skillId = player.getPersistentData().getInt("manhwamod.slot_" + i);
            g.renderOutline(x - 85, slotY, 170, 14, 0xFF00E5FF);
            g.drawString(this.font, "Slot " + i + ": " + getSkillName(player, skillId), x - 81, slotY + 3, 0xFFFFFFFF);
        }

        // 2. SCROLLABLE BANK (Bottom)
        g.drawCenteredString(this.font, "§a[ AVAILABLE BANK ]", x, y + 20, 0xFFFFFFFF);
        List<Integer> unlocked = SystemData.getUnlockedSkills(player);

        int maxVisibleRows = 3;
        int skillsPerRow = 2;
        int totalVisible = maxVisibleRows * skillsPerRow;

        for (int i = 0; i < totalVisible; i++) {
            // Calculate index based on scroll offset
            int index = (scrollOffset * skillsPerRow) + i;
            if (index >= unlocked.size()) break;

            int id = unlocked.get(index);
            int col = i % skillsPerRow;
            int row = i / skillsPerRow;

            int bx = (x - 85) + (col * 87);
            int by = (y + 35) + (row * 16); // Consistent spacing

            int color = (id == selectedSkillId) ? 0xFF00FF00 : 0xFF555555;
            g.renderOutline(bx, by, 83, 14, color);
            g.drawString(this.font, getSkillName(player, id), bx + 4, by + 3, 0xFFAAAAAA);
        }

        // Scroll Indicator (Business touch: let them know there is more)
        if (unlocked.size() > totalVisible) {
            g.drawString(this.font, "§7(Scroll for more)", x + 25, y + 85, 0xFF777777);
        }
    }

    private String getSkillName(Player player, int skillId) {
        if (skillId <= 0) return "§8- Empty -";

        // 1. Look for the Recipe String (e.g., "FIRE BLAST:COMMON:A manifested power")
        String recipe = player.getPersistentData().getString("manhwamod.skill_recipe_" + skillId);

        // 2. If the recipe is found, extract the Name (the part before the first :)
        if (!recipe.isEmpty()) {
            try {
                return recipe.split(":")[0];
            } catch (Exception e) {
                return "§7Unparsed Skill " + skillId;
            }
        }

        // 3. Fallback: If we have the ID but no recipe string, show the ID for debugging
        return "§c[No Data] Skill #" + skillId;
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
}