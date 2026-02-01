package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketCastSkill {
    private final int slotId;

    public PacketCastSkill(int slotId) {
        this.slotId = slotId;
    }

    public PacketCastSkill(FriendlyByteBuf buf) {
        this.slotId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slotId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            int skillId = player.getPersistentData().getInt(SystemData.SLOT_PREFIX + this.slotId);
            if (skillId <= 0) return;

            long lastUse = player.getPersistentData().getLong(SystemData.LAST_USE_PREFIX + this.slotId);
            int cooldownTime = player.getPersistentData().getInt(SystemData.COOLDOWN_PREFIX + this.slotId);
            long timePassed = player.level().getGameTime() - lastUse;

            if (timePassed < cooldownTime) return;

            String recipe = SystemData.getSkillRecipe(player, this.slotId);
            if (recipe.isEmpty()) return;

            // Trigger the monitor
            SkillEngine.scheduleCast(player, this.slotId, skillId, recipe);
        });
        return true;
    }
}