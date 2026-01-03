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
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        for (int i = 1; i <= 5; i++) {
            final int slot = i;
            // CASCADE: These now resolve because of the SystemData update
            int skillId = Minecraft.getInstance().player.getPersistentData().getInt(SystemData.SLOT_PREFIX + slot);
            String recipe = Minecraft.getInstance().player.getPersistentData().getString(SystemData.RECIPE_PREFIX + skillId);

            String displayName = recipe.isEmpty() ? "---" : recipe.split(":")[0];

            this.addRenderableWidget(Button.builder(Component.literal(slot + ": " + displayName), (button) -> {
                // CASCADE: Ensure Messages is reachable
                Messages.sendToServer(new PacketCastSkill(slot));
            }).bounds(centerX - 80, centerY - 60 + (i * 22), 160, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int current = player.getPersistentData().getInt(SystemData.CURRENT_MANA);
            int max = player.getPersistentData().getInt(SystemData.MANA);
            guiGraphics.drawCenteredString(this.font, "§bMANA: " + current + " / " + max, this.width / 2, y + 20, 0xFFFFFF);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}