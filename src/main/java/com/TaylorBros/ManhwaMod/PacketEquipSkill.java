package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketEquipSkill {
    private final int slot;   // 1-5
    private final int skillId; // The ID from the bank

    public PacketEquipSkill(int slot, int skillId) {
        this.slot = slot;
        this.skillId = skillId;
    }

    public PacketEquipSkill(FriendlyByteBuf buf) {
        this.slot = buf.readInt();
        this.skillId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        buf.writeInt(skillId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // SERVER SIDE: Save the mapping
            net.minecraft.server.level.ServerPlayer player = context.getSender();
            if (player != null) {
                // Save: Slot 1 now holds Skill ID 5
                player.getPersistentData().putInt("manhwamod.slot_" + slot, skillId);

                // Sync immediately so the UI sees the change
                SystemData.sync(player);
            }
        });
        return true;
    }
}