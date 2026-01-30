package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketPlayEffect {
    private final int entityId;
    private final String recipe;

    public PacketPlayEffect(int entityId, String recipe) {
        this.entityId = entityId;
        this.recipe = recipe;
    }

    public PacketPlayEffect(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.recipe = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(recipe);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            System.out.println("CLIENT RECEIVED EFFECT PACKET: " + recipe);
            // CLIENT SIDE: Find the player and play the effect
            if (Minecraft.getInstance().level != null) {
                Entity target = Minecraft.getInstance().level.getEntity(entityId);
                if (target instanceof Player player) {
                    SkillVisuals.play(player, "", recipe);
                }
            }
        });
        return true;
    }
}