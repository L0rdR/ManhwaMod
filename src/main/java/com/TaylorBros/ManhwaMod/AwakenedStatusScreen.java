package com.TaylorBros.ManhwaMod;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

public class AwakenedStatusScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ManhwaMod.MODID, "textures/gui/awakened_status.png");

    public AwakenedStatusScreen() {
        super(Component.literal("Awakened Status"));
    }

    @Override
    protected void init() {
        this.addButtons();
    }

    private void addButtons() {
        this.clearWidgets();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // RULE 1: Using Centralized Constants from SystemData
        for (int i = 1; i <= 5; i++) {
            int slot = i;
            int skillId = player.getPersistentData().getInt(SystemData.SLOT_PREFIX + slot);
            String recipe = player.getPersistentData().getString(SystemData.RECIPE_PREFIX + skillId);

            // RULE 3: Fail-safe naming
            String displayName = recipe.isEmpty() ? "[EMPTY SLOT]" : recipe.split(":")[0];

            this.addRenderableWidget(Button.builder(Component.literal(slot + ": " + displayName), (button) -> {
                Messages.sendToServer(new PacketCastSkill(slot));
            }).bounds(centerX - 100, centerY - 80 + (i * 25), 200, 20).build());
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack);
        RenderSystem.setShaderTexture(0, TEXTURE);

        // DRAW TEXT: Mana and Stats
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int current = player.getPersistentData().getInt(SystemData.CURRENT_MANA);
            int max = player.getPersistentData().getInt(SystemData.MANA);

            String manaText = "MANA: " + current + " / " + max;
            drawCenteredString(poseStack, this.font, manaText, this.width / 2, this.height / 2 - 100, 0x00FBFF);

            // BUSINESS LOGIC: If data was missing and suddenly appears, refresh buttons
            if (this.children().size() > 0 && current == 0 && max == 0) {
                // Potential sync lag - keep checking
            }
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}