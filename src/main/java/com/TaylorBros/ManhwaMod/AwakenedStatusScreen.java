package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class AwakenedStatusScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ManhwaMod.MODID, "textures/gui/awakened_status.png");
    private final int imageWidth = 256;
    private final int imageHeight = 256;

    public AwakenedStatusScreen() {
        super(Component.literal("Awakened Status"));
    }

    @Override
    protected void init() {
        this.addSkillButtons();
    }

    private void addSkillButtons() {
        this.clearWidgets();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // RULE 1: Using Centralized Constants from SystemData
        for (int i = 1; i <= 5; i++) {
            final int slot = i;
            int skillId = player.getPersistentData().getInt(SystemData.SLOT_PREFIX + slot);
            String recipe = player.getPersistentData().getString(SystemData.RECIPE_PREFIX + skillId);

            // Clean display name (Name:Rarity:Cost -> Name)
            String displayName = recipe.isEmpty() ? "---" : recipe.split(":")[0];

            this.addRenderableWidget(Button.builder(Component.literal(slot + ": " + displayName), (button) -> {
                Messages.sendToServer(new PacketCastSkill(slot));
            }).bounds(centerX - 80, centerY - 60 + (i * 22), 160, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Render Background Blur
        this.renderBackground(guiGraphics);

        // 2. Render Custom Texture
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // 3. Render Stats (Mana)
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int current = player.getPersistentData().getInt(SystemData.CURRENT_MANA);
            int max = player.getPersistentData().getInt(SystemData.MANA);
            int points = player.getPersistentData().getInt(SystemData.POINTS);

            guiGraphics.drawCenteredString(this.font, "§b§lMANA: §f" + current + " / " + max, this.width / 2, y + 20, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, "§e§lPOINTS: §f" + points, this.width / 2, y + 35, 0xFFFFFF);
        }

        // 4. Render Buttons/Widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}