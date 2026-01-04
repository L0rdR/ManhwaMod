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

    protected AwakenedStatusScreen() {
        super(Component.literal("Awakened Status"));
    }

    @Override
    protected void init() {
        int x = (this.width - WINDOW_WIDTH) / 2;
        int y = (this.height - WINDOW_HEIGHT) / 2;

        clearWidgets();

        addRenderableWidget(Button.builder(
                Component.literal(showSkills ? "VIEW STATS" : "VIEW SKILLS"),
                b -> {
                    showSkills = !showSkills;
                    init();
                }
        ).bounds(x + 10, y + WINDOW_HEIGHT - 25, 80, 18).build());

        if (!showSkills) {
            addRenderableWidget(Button.builder(
                    Component.literal("x" + multiplier),
                    b -> {
                        multiplier = multiplier == 1 ? 10 : multiplier == 10 ? 100 : 1;
                        b.setMessage(Component.literal("x" + multiplier));
                    }
            ).bounds(x + 135, y + 10, 45, 20).build());

            int bx = x + 160;
            int sy = y + 78;

            addStatButton(bx, sy, "STR");
            addStatButton(bx, sy + 20, "HP");
            addStatButton(bx, sy + 40, "DEF");
            addStatButton(bx, sy + 60, "SPD");
            addStatButton(bx, sy + 80, "MANA");
        }
    }

    // ==================================================
    // INPUT (FIXED)
    // ==================================================
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (showSkills && button == 0) {
            int x = (width - WINDOW_WIDTH) / 2;
            int y = (height - WINDOW_HEIGHT) / 2;

            // ===== CLR (Equipped Slots) =====
            for (int slot = 0; slot < 5; slot++) {
                int sx = x + 15 + slot * 34;
                int sy = y + 145;

                if (mouseX >= sx && mouseX <= sx + 30 &&
                        mouseY >= sy && mouseY <= sy + 30) {

                    int equipped = minecraft.player.getPersistentData()
                            .getInt(SystemData.SLOT_PREFIX + slot);

                    if (equipped != 0) {
                        Messages.sendToServer(new PacketEquipSkill(slot, 0));
                        return true; // 🚨 HARD STOP — DO NOT FALL THROUGH
                    }
                }
            }

            // ===== EQ BUTTONS =====
            List<Integer> skills = SystemData.getUnlockedSkills(minecraft.player);
            int listY = y + 35;

            for (int i = skillScrollOffset; i < Math.min(skills.size(), skillScrollOffset + 4); i++) {
                int rowY = listY + (i - skillScrollOffset) * 22;

                if (mouseX >= x + 150 && mouseX <= x + 172 &&
                        mouseY >= rowY + 1 && mouseY <= rowY + 19) {

                    equipToNextEmptySlot(skills.get(i));
                    return true;
                }
            }

            return true; // 🚨 BLOCK super() WHEN IN SKILLS TAB
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void addStatButton(int x, int y, String type) {
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            int pts = SystemData.getPoints(minecraft.player);
            int amt = Math.min(pts, multiplier);
            if (amt > 0)
                Messages.sendToServer(new PacketUpdateStats(amt, type));
        }).bounds(x, y, 20, 18).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g);

        int x = (width - WINDOW_WIDTH) / 2;
        int y = (height - WINDOW_HEIGHT) / 2;

        g.fill(x, y, x + WINDOW_WIDTH, y + WINDOW_HEIGHT, 0xAA000000);
        g.renderOutline(x, y, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF00AAFF);

        if (showSkills) renderSkillsTab(g, x, y, mouseX, mouseY);
        else renderStatsTab(g, x, y);

        super.render(g, mouseX, mouseY, pt);
    }
    private String clipText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;

        while (font.width(text + "...") > maxWidth && text.length() > 1) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }
    // ==================================================
    // SKILLS TAB
    // ==================================================
    private void renderSkillsTab(GuiGraphics g, int x, int y, int mx, int my) {
        g.drawString(font, "§b§lUNLOCKED ARTS", x + 12, y + 10, 0xFFFFFF);

        List<Integer> skills = SystemData.getUnlockedSkills(minecraft.player);
        int sy = y + 35;

        for (int i = skillScrollOffset; i < Math.min(skills.size(), skillScrollOffset + 4); i++) {
            int id = skills.get(i);
            String recipe = minecraft.player.getPersistentData()
                    .getString(SystemData.RECIPE_PREFIX + id);

            String name = clipText(SkillEngine.getSkillName(recipe), 115);

            boolean equipped = false;
            for (int s = 0; s < 5; s++)
                if (minecraft.player.getPersistentData()
                        .getInt(SystemData.SLOT_PREFIX + s) == id)
                    equipped = true;

            g.fill(x + 15, sy, x + 175, sy + 20, equipped ? 0x22888888 : 0x44FFFFFF);

            g.pose().pushPose();
            g.pose().translate(x + 20, sy + 6, 0);
            g.pose().scale(0.85f, 0.85f, 1);
            g.drawString(font, (equipped ? "§7" : "§e") + name, 0, 0, 0xFFFFFF);
            g.pose().popPose();

            if (!equipped) {
                g.fill(x + 150, sy + 1, x + 172, sy + 19, 0x6600AAFF);
                g.drawCenteredString(font, "EQ", x + 161, sy + 6, 0xFFFFFF);
            } else {
                g.drawString(font, "§a✔", x + 155, sy + 6, 0xFFFFFF);
            }

            sy += 22;
        }

        g.drawString(font, "§bEquipped Arts:", x + 15, y + 130, 0xFFFFFF);

        for (int s = 0; s < 5; s++) {
            int sx = x + 15 + s * 34;
            int sy2 = y + 145;
            int id = minecraft.player.getPersistentData()
                    .getInt(SystemData.SLOT_PREFIX + s);

            g.fill(sx, sy2, sx + 30, sy2 + 30, 0x66000000);
            g.renderOutline(sx, sy2, 30, 30, 0xFF00AAFF);

            if (id != 0) {
                String name = clipText(
                        SkillEngine.getSkillName(
                                minecraft.player.getPersistentData()
                                        .getString(SystemData.RECIPE_PREFIX + id)),
                        26
                );


                g.pose().pushPose();
                g.pose().translate(sx + 15, sy2 + 15, 0);
                g.pose().scale(0.6f, 0.6f, 1);
                g.drawCenteredString(font, name, 0, -4, 0xFFFFFF);
                g.pose().popPose();

                if (mx >= sx && mx <= sx + 30 && my >= sy2 && my <= sy2 + 30) {
                    g.fill(sx, sy2, sx + 30, sy2 + 30, 0x66FF0000);
                    g.drawCenteredString(font, "CLR", sx + 15, sy2 + 10, 0xFFFFFF);
                }
            } else {
                g.drawString(font, "§8" + (s + 1), sx + 12, sy2 + 10, 0xFFFFFF);
            }
        }
    }

    private void equipToNextEmptySlot(int skillId) {
        for (int s = 0; s < 5; s++) {
            int cur = minecraft.player.getPersistentData()
                    .getInt(SystemData.SLOT_PREFIX + s);

            if (cur == skillId) return;
            if (cur == 0) {
                Messages.sendToServer(new PacketEquipSkill(s, skillId));
                return;
            }
        }
    }

    // ==================================================
    // STATS TAB
    // ==================================================
    private void renderStatsTab(GuiGraphics g, int x, int y) {
        g.drawString(font, "§b§lAWAKENED - " +
                        minecraft.player.getName().getString().toUpperCase(),
                x + 12, y + 10, 0xFFFFFF);

        int lvl = minecraft.player.getPersistentData().getInt(SystemData.LEVEL);
        String rank = minecraft.player.getPersistentData().getString("manhwamod.rank");
        if (rank.isEmpty()) rank = "E";

        g.drawString(font, "§fRank: " + getRankColor(rank) + rank,
                x + 15, y + 25, 0xFFFFFF);

        g.drawString(font, "§fLevel: §b" + lvl + "§7/1000",
                x + 15, y + 35, 0xFFFFFF);

        g.fill(x + 15, y + 46, x + 175, y + 48, 0xFF444444);
        g.fill(x + 15, y + 46,
                x + 15 + (int) (160 * (lvl / 1000f)),
                y + 48, 0xFF00AAFF);

        g.drawString(font, "§fAvailable Points: §e" +
                        SystemData.getPoints(minecraft.player),
                x + 15, y + 65, 0xFFFFFF);

        drawStat(g, "Strength:", SystemData.getStrength(minecraft.player), "§c", x + 15, y + 80);
        drawStat(g, "Health:", SystemData.getHealthStat(minecraft.player), "§a", x + 15, y + 100);
        drawStat(g, "Defense:", SystemData.getDefense(minecraft.player), "§7", x + 15, y + 120);
        drawStat(g, "Speed:", SystemData.getSpeed(minecraft.player), "§f", x + 15, y + 140);
        drawStat(g, "Mana:", SystemData.getMana(minecraft.player), "§d", x + 15, y + 160);
    }

    private void drawStat(GuiGraphics g, String name, int value, String color, int x, int y) {
        g.drawString(font, name, x, y, 0xFFFFFF);
        g.drawString(font, color + value, x + 80, y, 0xFFFFFF);
    }

    private String getRankColor(String r) {
        return switch (r) {
            case "S" -> "§6";
            case "A" -> "§c";
            case "B" -> "§a";
            case "C" -> "§b";
            case "D" -> "§f";
            default -> "§7";
        };
    }
}
