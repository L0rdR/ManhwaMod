package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketPlayStrikeImpact {

    private final int targetId;
    private final String recipe;

    public PacketPlayStrikeImpact(int targetId, String recipe) {
        this.targetId = targetId;
        this.recipe = recipe;
    }

    public PacketPlayStrikeImpact(FriendlyByteBuf buf) {
        this.targetId = buf.readInt();
        this.recipe = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(targetId);
        buf.writeUtf(recipe);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;

            // Client-side: spawn a quick impact cut over the hit target.
            StrikeVisuals.playImpact(Minecraft.getInstance().level, targetId, recipe);
        });
        return true;
    }
}
